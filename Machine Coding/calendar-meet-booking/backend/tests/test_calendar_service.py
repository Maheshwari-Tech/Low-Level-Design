from datetime import date, datetime
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier

import pytest

from app.application import CalendarService
from app.domain import CalendarError, MeetingConflictError
from app.infrastructure import InMemoryCalendarRepository


def test_touching_meetings_do_not_conflict():
    service = CalendarService(InMemoryCalendarRepository())
    service.schedule(
        "First",
        ["ada@example.com"],
        datetime(2026, 9, 22, 10),
        datetime(2026, 9, 22, 10, 30),
    )
    second = service.schedule(
        "Second",
        ["ada@example.com"],
        datetime(2026, 9, 22, 10, 30),
        datetime(2026, 9, 22, 11),
    )
    assert second.title == "Second"


def test_shared_participant_cannot_have_overlapping_meetings():
    service = CalendarService(InMemoryCalendarRepository())
    service.schedule(
        "First",
        ["ada@example.com"],
        datetime(2026, 9, 22, 10),
        datetime(2026, 9, 22, 10, 30),
    )
    with pytest.raises(MeetingConflictError):
        service.schedule(
            "Conflict",
            ["ada@example.com", "bob@example.com"],
            datetime(2026, 9, 22, 10, 15),
            datetime(2026, 9, 22, 10, 45),
        )


def test_availability_returns_sixteen_half_hour_slots():
    service = CalendarService(InMemoryCalendarRepository())
    slots = service.availability(date(2026, 9, 22), ["ada@example.com"])
    assert len(slots) == 16
    assert all(available for _, available in slots)


def test_participant_identity_is_normalized_before_conflict_check():
    service = CalendarService(InMemoryCalendarRepository())
    service.schedule(
        "First",
        [" Ada@Example.com "],
        datetime(2026, 9, 22, 10),
        datetime(2026, 9, 22, 10, 30),
    )

    with pytest.raises(MeetingConflictError):
        service.schedule(
            "Conflict",
            ["ada@example.com"],
            datetime(2026, 9, 22, 10, 15),
            datetime(2026, 9, 22, 10, 45),
        )


def test_meeting_outside_working_hours_is_rejected():
    service = CalendarService(InMemoryCalendarRepository())

    with pytest.raises(CalendarError, match="working hours"):
        service.schedule(
            "Too early",
            ["ada@example.com"],
            datetime(2026, 9, 22, 8, 30),
            datetime(2026, 9, 22, 9, 30),
        )


def test_availability_requires_a_participant():
    service = CalendarService(InMemoryCalendarRepository())

    with pytest.raises(CalendarError, match="participant"):
        service.availability(date(2026, 9, 22), ["  "])


def test_concurrent_conflicting_requests_create_exactly_one_meeting():
    service = CalendarService(InMemoryCalendarRepository())
    start_together = Barrier(2)

    def attempt(title: str) -> bool:
        start_together.wait()
        try:
            service.schedule(
                title,
                ["ada@example.com"],
                datetime(2026, 9, 22, 10),
                datetime(2026, 9, 22, 10, 30),
            )
            return True
        except MeetingConflictError:
            return False

    with ThreadPoolExecutor(max_workers=2) as executor:
        results = list(executor.map(attempt, ["First", "Second"]))

    assert sorted(results) == [False, True]
    assert len(service.list_meetings()) == 1
