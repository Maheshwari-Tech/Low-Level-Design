from app.exceptions.game_exceptions import (
    DuplicateShotError,
    InvalidPlacementError,
    InvalidShotError,
)
from app.models.cell import Cell
from app.models.enums import Direction, ShotStatus
from app.models.ship import Ship
from app.models.shot import Shot


class Board:
    def __init__(self, size: int) -> None:
        if size < 1:
            raise ValueError("Board size must be positive")
        self._size = size
        self._ships: dict[str, Ship] = {}
        self._shots: dict[Cell, Shot] = {}

    @property
    def size(self) -> int:
        return self._size

    @property
    def ships(self) -> tuple[Ship, ...]:
        return tuple(self._ships.values())

    @property
    def shots(self) -> tuple[Shot, ...]:
        return tuple(self._shots.values())

    @property
    def all_ships_sunk(self) -> bool:
        return bool(self._ships) and all(ship.is_sunk for ship in self._ships.values())

    def place_ship(self, ship: Ship, origin: Cell, direction: Direction) -> None:
        occupied_cells = {
            cell for placed_ship in self.ships for cell in placed_ship.cells
        }
        proposed_cells = self._calculate_cells(origin, ship.size, direction)
        if occupied_cells.intersection(proposed_cells):
            raise InvalidPlacementError("Ships cannot overlap")
        ship.place_at(proposed_cells)
        self._ships[ship.name] = ship

    def receive_shot(self, target: Cell) -> Shot:
        if not self._contains(target):
            raise InvalidShotError("Shot is outside the board")
        if target in self._shots:
            raise DuplicateShotError("That cell was already targeted")

        hit_ship = next((ship for ship in self.ships if ship.occupies(target)), None)
        if hit_ship is None:
            shot = Shot(target=target, status=ShotStatus.MISS)
        else:
            hit_ship.register_hit(target)
            shot = Shot(
                target=target,
                status=ShotStatus.HIT,
                ship_name=hit_ship.name,
            )
        self._shots[target] = shot
        return shot

    def _calculate_cells(
        self, origin: Cell, ship_size: int, direction: Direction
    ) -> tuple[Cell, ...]:
        cells = tuple(
            Cell(
                row=origin.row + (offset if direction is Direction.VERTICAL else 0),
                column=origin.column
                + (offset if direction is Direction.HORIZONTAL else 0),
            )
            for offset in range(ship_size)
        )
        for cell in cells:
            if not self._contains(cell):
                raise InvalidPlacementError("Ship does not fit on the board")
        return cells

    def _contains(self, cell: Cell) -> bool:
        return 0 <= cell.row < self._size and 0 <= cell.column < self._size
