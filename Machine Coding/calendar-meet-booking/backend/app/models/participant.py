from dataclasses import dataclass

from app.exceptions.calendar_exceptions import CalendarError


@dataclass(frozen=True, order=True)
class Participant:
    email: str

    def __post_init__(self) -> None:
        normalized_email = self.email.strip().casefold()
        if not normalized_email:
            raise CalendarError("Participant email is required")
        object.__setattr__(self, "email", normalized_email)
