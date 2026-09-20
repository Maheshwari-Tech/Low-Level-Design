from datetime import datetime, timedelta, timezone
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier

import pytest

from app.application import BookingService
from app.domain import HoldExpiredError, NotFoundError, SeatUnavailableError
from app.infrastructure import InMemoryBookingRepository

NOW = datetime(2026, 9, 20, tzinfo=timezone.utc)


def service() -> BookingService:
    return BookingService(InMemoryBookingRepository(), clock=lambda: NOW)


def test_hold_then_confirm_books_seats():
    bookings = service()
    hold = bookings.hold_seats("show-1", ["A1", "A2"])
    booking = bookings.confirm_booking("show-1", hold.id, "Ada Lovelace")
    assert booking.seats == frozenset({"A1", "A2"})
    assert str(booking.total) == "560"


def test_same_seat_cannot_be_held_twice():
    bookings = service()
    bookings.hold_seats("show-1", ["A1"])
    with pytest.raises(SeatUnavailableError):
        bookings.hold_seats("show-1", ["A1"])


def test_expired_hold_cannot_be_confirmed():
    current = NOW
    bookings = BookingService(
        InMemoryBookingRepository(),
        clock=lambda: current,
        hold_duration=timedelta(seconds=1),
    )
    hold = bookings.hold_seats("show-1", ["A1"])
    current += timedelta(seconds=2)
    with pytest.raises(HoldExpiredError):
        bookings.confirm_booking("show-1", hold.id, "Ada Lovelace")


def test_hold_cannot_be_confirmed_twice():
    bookings = service()
    hold = bookings.hold_seats("show-1", ["A1"])
    bookings.confirm_booking("show-1", hold.id, "Ada Lovelace")
    with pytest.raises(NotFoundError):
        bookings.confirm_booking("show-1", hold.id, "Ada Lovelace")


def test_seat_ids_are_normalized_at_the_use_case_boundary():
    bookings = service()
    hold = bookings.hold_seats("show-1", [" a1 "])

    assert hold.seats == frozenset({"A1"})


def test_expired_hold_releases_the_seat_for_a_new_hold():
    current = NOW
    bookings = BookingService(
        InMemoryBookingRepository(),
        clock=lambda: current,
        hold_duration=timedelta(seconds=1),
    )
    bookings.hold_seats("show-1", ["A1"])
    current += timedelta(seconds=2)

    replacement = bookings.hold_seats("show-1", ["A1"])

    assert replacement.seats == frozenset({"A1"})


def test_blank_customer_name_is_rejected_even_without_http_validation():
    bookings = service()
    hold = bookings.hold_seats("show-1", ["A1"])

    with pytest.raises(ValueError, match="Customer name"):
        bookings.confirm_booking("show-1", hold.id, "  ")


def test_concurrent_holds_for_one_seat_have_exactly_one_winner():
    bookings = service()
    start_together = Barrier(2)

    def attempt(_request_number: int) -> bool:
        start_together.wait()
        try:
            bookings.hold_seats("show-1", ["A1"])
            return True
        except SeatUnavailableError:
            return False

    with ThreadPoolExecutor(max_workers=2) as executor:
        results = list(executor.map(attempt, [1, 2]))

    assert sorted(results) == [False, True]
