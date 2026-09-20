from collections.abc import Mapping

from app.exceptions.game_exceptions import UnknownShipError
from app.models.ship import Ship


class ShipFactory:
    DEFAULT_FLEET = {
        "Destroyer": 2,
        "Cruiser": 3,
        "Battleship": 4,
    }

    def __init__(self, fleet_sizes: Mapping[str, int] | None = None) -> None:
        configured_fleet = self.DEFAULT_FLEET if fleet_sizes is None else fleet_sizes
        if not configured_fleet:
            raise ValueError("Fleet must contain at least one ship")
        if any(not name.strip() or size < 1 for name, size in configured_fleet.items()):
            raise ValueError("Ship names must be non-empty and sizes must be positive")

        normalized_names = [name.casefold() for name in configured_fleet]
        if len(normalized_names) != len(set(normalized_names)):
            raise ValueError("Ship names must be unique ignoring case")

        self._fleet_sizes = dict(configured_fleet)

    @property
    def required_ship_names(self) -> tuple[str, ...]:
        return tuple(self._fleet_sizes)

    @property
    def fleet_sizes(self) -> dict[str, int]:
        return dict(self._fleet_sizes)

    def create(self, requested_name: str) -> Ship:
        canonical_name = next(
            (
                name
                for name in self._fleet_sizes
                if name.casefold() == requested_name.casefold()
            ),
            None,
        )
        if canonical_name is None:
            raise UnknownShipError(f"Unknown ship: {requested_name}")
        return Ship(canonical_name, self._fleet_sizes[canonical_name])
