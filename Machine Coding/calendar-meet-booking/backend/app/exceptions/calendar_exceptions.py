class CalendarError(ValueError):
    """Base class for expected calendar failures."""


class MeetingConflictError(CalendarError):
    pass
