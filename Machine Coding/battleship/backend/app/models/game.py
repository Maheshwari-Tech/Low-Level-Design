from threading import RLock

from app.exceptions.game_exceptions import (
    FleetIncompleteError,
    GameAlreadyStartedError,
    GameFinishedError,
    ShipAlreadyPlacedError,
)
from app.factories.ship_factory import ShipFactory
from app.models.board import Board
from app.models.cell import Cell
from app.models.enums import Direction, GameStatus
from app.models.shot import ShotOutcome


class BattleshipGame:
    def __init__(self, board: Board, ship_factory: ShipFactory) -> None:
        self._board = board
        self._ship_factory = ship_factory
        self._status = GameStatus.PLACING_FLEET
        self._lock = RLock()

    @property
    def board(self) -> Board:
        return self._board

    @property
    def status(self) -> GameStatus:
        return self._status

    @property
    def is_ready(self) -> bool:
        placed_ship_names = {ship.name for ship in self._board.ships}
        return placed_ship_names == set(self._ship_factory.required_ship_names)

    @property
    def fleet_sizes(self) -> dict[str, int]:
        return self._ship_factory.fleet_sizes

    def place_ship(self, name: str, origin: Cell, direction: Direction) -> None:
        with self._lock:
            if self._status in (GameStatus.IN_PROGRESS, GameStatus.WON):
                raise GameAlreadyStartedError(
                    "The game has started; ships can no longer be moved"
                )
            if any(
                ship.name.casefold() == name.casefold() for ship in self._board.ships
            ):
                raise ShipAlreadyPlacedError(f"{name} is already placed")

            ship = self._ship_factory.create(name)
            self._board.place_ship(ship, origin, direction)
            if self.is_ready:
                self._status = GameStatus.READY

    def fire(self, target: Cell) -> ShotOutcome:
        with self._lock:
            if self._status is GameStatus.WON:
                raise GameFinishedError("The game is already won")
            if not self.is_ready:
                raise FleetIncompleteError("Place the complete fleet before firing")

            shot = self._board.receive_shot(target)
            self._status = GameStatus.IN_PROGRESS
            sunk_ship = next(
                (
                    ship.name
                    for ship in self._board.ships
                    if ship.name == shot.ship_name and ship.is_sunk
                ),
                None,
            )
            won = self._board.all_ships_sunk
            if won:
                self._status = GameStatus.WON
            return ShotOutcome(shot=shot, sunk_ship_name=sunk_ship, won=won)
