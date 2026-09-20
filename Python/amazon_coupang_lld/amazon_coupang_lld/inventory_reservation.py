from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from enum import Enum, auto
from threading import RLock
from uuid import UUID, uuid4


class ReservationStatus(Enum):
    ACTIVE = auto()
    CONFIRMED = auto()
    RELEASED = auto()
    EXPIRED = auto()


@dataclass(frozen=True)
class RequestLine:
    sku: str
    quantity: int

    def __post_init__(self) -> None:
        if self.quantity <= 0:
            raise ValueError("quantity must be positive")


@dataclass
class Stock:
    on_hand: int
    reserved: int = 0

    @property
    def available(self) -> int:
        return self.on_hand - self.reserved


@dataclass
class Reservation:
    id: UUID
    lines: tuple[RequestLine, ...]
    expires_at: datetime
    status: ReservationStatus = ReservationStatus.ACTIVE


class InventoryService:
    """One lock models a serializable unit of work; production uses row/conditional writes."""

    def __init__(self) -> None:
        self._lock = RLock()
        self._stock: dict[str, Stock] = {}
        self._reservations: dict[UUID, Reservation] = {}

    def add_stock(self, sku: str, quantity: int) -> None:
        with self._lock:
            stock = self._stock.setdefault(sku, Stock(0))
            stock.on_hand += quantity

    def reserve(self, requested: list[RequestLine], ttl: timedelta) -> Reservation:
        if not requested:
            raise ValueError("reservation requires at least one line")
        totals: dict[str, int] = {}
        for line in requested:
            totals[line.sku] = totals.get(line.sku, 0) + line.quantity
        lines = tuple(RequestLine(sku, totals[sku]) for sku in sorted(totals))
        with self._lock:
            for line in lines:
                if self._stock[line.sku].available < line.quantity:
                    raise RuntimeError(f"insufficient {line.sku}")
            for line in lines:
                self._stock[line.sku].reserved += line.quantity
            reservation = Reservation(uuid4(), lines, datetime.now(timezone.utc) + ttl)
            self._reservations[reservation.id] = reservation
            return reservation

    def confirm(self, reservation_id: UUID) -> bool:
        with self._lock:
            reservation = self._reservations[reservation_id]
            if reservation.status is ReservationStatus.CONFIRMED:
                return True
            if reservation.status is not ReservationStatus.ACTIVE:
                return False
            for line in reservation.lines:
                stock = self._stock[line.sku]
                stock.reserved -= line.quantity
                stock.on_hand -= line.quantity
            reservation.status = ReservationStatus.CONFIRMED
            return True

    def expire_due(self, now: datetime) -> int:
        with self._lock:
            expired = 0
            for reservation in self._reservations.values():
                if reservation.status is ReservationStatus.ACTIVE and reservation.expires_at <= now:
                    for line in reservation.lines:
                        self._stock[line.sku].reserved -= line.quantity
                    reservation.status = ReservationStatus.EXPIRED
                    expired += 1
            return expired

    def available(self, sku: str) -> int:
        with self._lock:
            return self._stock[sku].available


def run_demo() -> None:
    service = InventoryService()
    service.add_stock("A", 10)

    def contend() -> bool:
        try:
            service.reserve([RequestLine("A", 1)], timedelta(minutes=1))
            return True
        except RuntimeError:
            return False

    with ThreadPoolExecutor(max_workers=16) as pool:
        successes = sum(pool.map(lambda _: contend(), range(100)))
    assert successes == 10 and service.available("A") == 0
    print("Python Inventory Reservation: passed (no oversell)")
