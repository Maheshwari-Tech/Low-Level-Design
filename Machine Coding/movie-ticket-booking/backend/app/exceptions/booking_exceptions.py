class BookingError(ValueError):
    """Base class for expected booking failures."""


class ShowNotFoundError(BookingError):
    pass


class HoldNotFoundError(BookingError):
    pass


class SeatUnavailableError(BookingError):
    pass


class HoldExpiredError(BookingError):
    pass


class InvalidBookingError(BookingError):
    pass
