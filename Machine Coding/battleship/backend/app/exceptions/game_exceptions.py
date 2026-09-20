class BattleshipError(Exception):
    """Base class for expected game-rule violations."""


class UnknownShipError(BattleshipError):
    pass


class ShipAlreadyPlacedError(BattleshipError):
    pass


class InvalidPlacementError(BattleshipError):
    pass


class InvalidShotError(BattleshipError):
    pass


class FleetIncompleteError(BattleshipError):
    pass


class DuplicateShotError(BattleshipError):
    pass


class GameAlreadyStartedError(BattleshipError):
    pass


class GameFinishedError(BattleshipError):
    pass
