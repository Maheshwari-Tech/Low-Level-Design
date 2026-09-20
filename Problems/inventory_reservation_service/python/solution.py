"""Interview-sized inventory core with atomic holds and precise expiry.

Ordered per-inventory-key locks model the storage transaction.  A real repository
must atomically persist balances, reservation, idempotency receipt, and outbox row.
"""

from __future__ import annotations

from contextlib import contextmanager
from dataclasses import dataclass, replace
from datetime import datetime, timedelta
from enum import Enum
from threading import RLock
from typing import Dict, Iterator, Mapping, Optional, Protocol, Tuple


class InventoryError(Exception):
    pass


class InsufficientInventory(InventoryError):
    pass


class InvalidAllocation(InventoryError):
    pass


class IdempotencyConflict(InventoryError):
    pass


@dataclass(frozen=True, order=True)
class InventoryKey:
    sku: str
    location_id: str

    def __post_init__(self) -> None:
        if not self.sku.strip() or not self.location_id.strip():
            raise ValueError("SKU and location must not be blank")


@dataclass(frozen=True)
class InventoryBalance:
    key: InventoryKey
    on_hand: int
    reserved: int
    version: int

    def __post_init__(self) -> None:
        if self.on_hand < 0 or self.reserved < 0 or self.reserved > self.on_hand:
            raise ValueError("balance must satisfy 0 <= reserved <= on_hand")

    @property
    def available(self) -> int:
        return self.on_hand - self.reserved


@dataclass
class _Balance:
    on_hand: int
    reserved: int = 0
    version: int = 1

    def snapshot(self, key: InventoryKey) -> InventoryBalance:
        return InventoryBalance(key, self.on_hand, self.reserved, self.version)

    def hold(self, quantity: int) -> None:
        if quantity <= 0 or quantity > self.on_hand - self.reserved:
            raise InsufficientInventory("not enough available inventory")
        self.reserved += quantity
        self.version += 1

    def finish(self, quantity: int, consume: bool) -> None:
        if quantity <= 0 or quantity > self.reserved:
            raise AssertionError("reservation counters are inconsistent")
        self.reserved -= quantity
        if consume:
            self.on_hand -= quantity
        self.version += 1


@dataclass(frozen=True)
class Allocation:
    key: InventoryKey
    quantity: int

    def __post_init__(self) -> None:
        if self.quantity <= 0:
            raise ValueError("allocation quantity must be positive")


class ReservationStatus(Enum):
    ACTIVE = "ACTIVE"
    CONFIRMED = "CONFIRMED"
    RELEASED = "RELEASED"
    EXPIRED = "EXPIRED"


@dataclass(frozen=True)
class Reservation:
    reservation_id: str
    requested: Tuple[Tuple[str, int], ...]
    allocations: Tuple[Allocation, ...]
    created_at: datetime
    expires_at: datetime
    status: ReservationStatus = ReservationStatus.ACTIVE
    version: int = 1

    def __post_init__(self) -> None:
        if not self.reservation_id or not self.requested or not self.allocations:
            raise ValueError("reservation ID, request, and allocations are required")
        if self.expires_at <= self.created_at:
            raise ValueError("reservation expiry must be after creation")

    def is_due(self, now: datetime) -> bool:
        return now >= self.expires_at

    def terminal(self, status: ReservationStatus) -> "Reservation":
        if self.status is not ReservationStatus.ACTIVE or status is ReservationStatus.ACTIVE:
            raise InventoryError("invalid reservation transition")
        return replace(self, status=status, version=self.version + 1)


class AllocationStrategy(Protocol):
    def allocate(
        self, requested: Mapping[str, int], balances: Tuple[InventoryBalance, ...]
    ) -> Tuple[Allocation, ...]:
        """Return a pure proposal; the service owns validation and mutation."""


class FirstFitAllocationStrategy:
    """Deterministic first-fit by SKU, then location."""

    def allocate(
        self, requested: Mapping[str, int], balances: Tuple[InventoryBalance, ...]
    ) -> Tuple[Allocation, ...]:
        by_sku: Dict[str, list] = {}
        for balance in sorted(balances, key=lambda item: item.key):
            by_sku.setdefault(balance.key.sku, []).append(balance)
        proposed = []
        for sku, wanted in sorted(requested.items()):
            remaining = wanted
            for balance in by_sku.get(sku, []):
                quantity = min(remaining, balance.available)
                if quantity:
                    proposed.append(Allocation(balance.key, quantity))
                    remaining -= quantity
                if not remaining:
                    break
            if remaining:
                raise InsufficientInventory(
                    "SKU {} is short by {} unit(s)".format(sku, remaining)
                )
        return tuple(proposed)


class Clock(Protocol):
    def now(self) -> datetime:
        ...


class ReservationIds(Protocol):
    def next_id(self) -> str:
        ...


class CommandOutcome(Enum):
    APPLIED = "APPLIED"
    REJECTED = "REJECTED"
    EXPIRED = "EXPIRED"
    CONFLICT = "CONFLICT"


@dataclass(frozen=True)
class CommandResult:
    outcome: CommandOutcome
    reservation: Optional[Reservation]
    message: str = ""


@dataclass(frozen=True)
class _Receipt:
    fingerprint: Tuple[object, ...]
    result: CommandResult


class InventoryReservationService:
    """Locks every touched key in sorted order before check-and-mutate."""

    def __init__(
        self,
        clock: Clock,
        reservation_ids: ReservationIds,
        strategy: Optional[AllocationStrategy] = None,
    ) -> None:
        self._clock, self._ids = clock, reservation_ids
        self._strategy = strategy or FirstFitAllocationStrategy()
        self._balances: Dict[InventoryKey, _Balance] = {}
        self._reservations: Dict[str, Reservation] = {}
        self._receipts: Dict[Tuple[str, str], _Receipt] = {}
        self._key_locks: Dict[InventoryKey, RLock] = {}
        self._command_locks: Dict[Tuple[str, str], RLock] = {}
        self._meta_lock = RLock()

    def set_on_hand(self, key: InventoryKey, quantity: int) -> InventoryBalance:
        """Trusted adjustment port; active holds may not be invalidated."""
        if quantity < 0:
            raise ValueError("on-hand must not be negative")
        with self._lock_keys((key,)):
            with self._meta_lock:
                balance = self._balances.get(key)
                if balance is None:
                    balance = self._balances[key] = _Balance(quantity)
                    created = True
                else:
                    created = False
            if not created:
                if quantity < balance.reserved:
                    raise InventoryError("on-hand cannot fall below reserved")
                balance.on_hand, balance.version = quantity, balance.version + 1
            return balance.snapshot(key)

    def reserve(
        self, key: str, requested: Mapping[str, int], ttl: timedelta
    ) -> CommandResult:
        canonical = self._canonical_request(requested)
        if ttl <= timedelta(0):
            raise ValueError("TTL must be positive")
        scope, fingerprint = self._scope("reserve", key), (canonical, ttl)
        with self._command_lock(scope):
            replay = self._replay(scope, fingerprint)
            if replay:
                return replay
            requested_by_sku = dict(canonical)
            with self._meta_lock:
                candidates = tuple(sorted(
                    inventory_key for inventory_key in self._balances
                    if inventory_key.sku in requested_by_sku
                ))

            # Allocation is only a proposal; no counter moves until every SKU fits.
            with self._lock_keys(candidates):
                snapshots = tuple(
                    self._balances[inventory_key].snapshot(inventory_key)
                    for inventory_key in candidates
                )
                try:
                    proposed = self._strategy.allocate(requested_by_sku, snapshots)
                    allocations = self._validate(requested_by_sku, snapshots, proposed)
                except InsufficientInventory as error:
                    return self._remember(scope, fingerprint, CommandResult(
                        CommandOutcome.REJECTED, None, str(error)
                    ))
                now = self._clock.now()
                reservation = Reservation(
                    self._ids.next_id(), canonical, allocations, now, now + ttl
                )
                with self._meta_lock:
                    if reservation.reservation_id in self._reservations:
                        raise ValueError("duplicate generated reservation ID")
                    for allocation in allocations:
                        self._balances[allocation.key].hold(allocation.quantity)
                    self._reservations[reservation.reservation_id] = reservation
                return self._remember(scope, fingerprint, CommandResult(
                    CommandOutcome.APPLIED, reservation
                ))

    def confirm(self, key: str, reservation_id: str) -> CommandResult:
        return self._terminal_command("confirm", key, reservation_id)

    def release(self, key: str, reservation_id: str) -> CommandResult:
        return self._terminal_command("release", key, reservation_id)

    def expire_due(self) -> Tuple[Reservation, ...]:
        """The guarded transition makes repeated or concurrent sweeps harmless."""
        now = self._clock.now()
        with self._meta_lock:
            due_ids = tuple(
                item.reservation_id for item in self._reservations.values()
                if item.status is ReservationStatus.ACTIVE and item.is_due(now)
            )
        expired = []
        for reservation_id in due_ids:
            reservation = self._require_reservation(reservation_id)
            with self._lock_keys(self._keys(reservation)):
                current = self._require_reservation(reservation_id)
                if current.status is ReservationStatus.ACTIVE and current.is_due(now):
                    expired.append(self._finish_active(current, ReservationStatus.EXPIRED))
        return tuple(expired)

    def get_reservation(self, reservation_id: str) -> Reservation:
        return self._require_reservation(reservation_id)

    def get_balance(self, key: InventoryKey) -> InventoryBalance:
        with self._lock_keys((key,)):
            balance = self._balances.get(key)
            return balance.snapshot(key) if balance else InventoryBalance(key, 0, 0, 0)

    def _terminal_command(
        self, operation: str, key: str, reservation_id: str
    ) -> CommandResult:
        scope, fingerprint = self._scope(operation, key), (reservation_id,)
        with self._command_lock(scope):
            replay = self._replay(scope, fingerprint)
            if replay:
                return replay
            reservation = self._require_reservation(reservation_id)
            with self._lock_keys(self._keys(reservation)):
                current = self._require_reservation(reservation_id)
                now = self._clock.now()
                if current.status is ReservationStatus.ACTIVE and current.is_due(now):
                    expired = self._finish_active(current, ReservationStatus.EXPIRED)
                    result = CommandResult(CommandOutcome.EXPIRED, expired, "TTL elapsed")
                elif current.status is ReservationStatus.ACTIVE:
                    target = (
                        ReservationStatus.CONFIRMED
                        if operation == "confirm" else ReservationStatus.RELEASED
                    )
                    changed = self._finish_active(current, target)
                    result = CommandResult(CommandOutcome.APPLIED, changed)
                elif (
                    operation == "confirm" and current.status is ReservationStatus.CONFIRMED
                ) or (
                    operation == "release" and current.status is ReservationStatus.RELEASED
                ):
                    result = CommandResult(CommandOutcome.APPLIED, current, "already applied")
                elif current.status is ReservationStatus.EXPIRED:
                    result = CommandResult(CommandOutcome.EXPIRED, current, "TTL elapsed")
                else:
                    result = CommandResult(CommandOutcome.CONFLICT, current, "terminal conflict")
                return self._remember(scope, fingerprint, result)

    def _finish_active(
        self, reservation: Reservation, target: ReservationStatus
    ) -> Reservation:
        consume = target is ReservationStatus.CONFIRMED
        for allocation in reservation.allocations:
            self._balances[allocation.key].finish(allocation.quantity, consume)
        changed = reservation.terminal(target)
        with self._meta_lock:
            self._reservations[reservation.reservation_id] = changed
        return changed

    @staticmethod
    def _validate(
        requested: Mapping[str, int],
        snapshots: Tuple[InventoryBalance, ...],
        proposed: Tuple[Allocation, ...],
    ) -> Tuple[Allocation, ...]:
        available = {item.key: item.available for item in snapshots}
        by_key: Dict[InventoryKey, int] = {}
        by_sku: Dict[str, int] = {}
        for allocation in proposed:
            if allocation.key not in available or allocation.key.sku not in requested:
                raise InvalidAllocation("strategy returned an ineligible key")
            by_key[allocation.key] = by_key.get(allocation.key, 0) + allocation.quantity
            by_sku[allocation.key.sku] = by_sku.get(allocation.key.sku, 0) + allocation.quantity
        if by_sku != dict(requested):
            raise InvalidAllocation("proposal does not exactly satisfy the request")
        if any(quantity > available[key] for key, quantity in by_key.items()):
            raise InvalidAllocation("proposal exceeds availability")
        return tuple(Allocation(key, quantity) for key, quantity in sorted(by_key.items()))

    @staticmethod
    def _canonical_request(requested: Mapping[str, int]) -> Tuple[Tuple[str, int], ...]:
        if not requested:
            raise ValueError("at least one SKU is required")
        if any(not sku.strip() or quantity <= 0 for sku, quantity in requested.items()):
            raise ValueError("SKUs must be non-blank and quantities positive")
        return tuple(sorted(requested.items()))

    @staticmethod
    def _keys(reservation: Reservation) -> Tuple[InventoryKey, ...]:
        return tuple(allocation.key for allocation in reservation.allocations)

    def _require_reservation(self, reservation_id: str) -> Reservation:
        with self._meta_lock:
            reservation = self._reservations.get(reservation_id)
        if reservation is None:
            raise InventoryError("unknown reservation {}".format(reservation_id))
        return reservation

    @staticmethod
    def _scope(operation: str, key: str) -> Tuple[str, str]:
        if not key or not key.strip():
            raise ValueError("idempotency key must not be blank")
        return operation, key

    def _replay(
        self, scope: Tuple[str, str], fingerprint: Tuple[object, ...]
    ) -> Optional[CommandResult]:
        with self._meta_lock:
            receipt = self._receipts.get(scope)
        if receipt is None:
            return None
        if receipt.fingerprint != fingerprint:
            raise IdempotencyConflict("key reused with different input")
        return receipt.result

    def _remember(
        self, scope: Tuple[str, str], fingerprint: Tuple[object, ...], result: CommandResult
    ) -> CommandResult:
        with self._meta_lock:
            self._receipts[scope] = _Receipt(fingerprint, result)
        return result

    def _command_lock(self, scope: Tuple[str, str]) -> RLock:
        with self._meta_lock:
            return self._command_locks.setdefault(scope, RLock())

    def _key_lock(self, key: InventoryKey) -> RLock:
        with self._meta_lock:
            return self._key_locks.setdefault(key, RLock())

    @contextmanager
    def _lock_keys(self, keys: Tuple[InventoryKey, ...]) -> Iterator[None]:
        locks = [self._key_lock(key) for key in sorted(set(keys))]
        for lock in locks:
            lock.acquire()
        try:
            yield
        finally:
            for lock in reversed(locks):
                lock.release()
