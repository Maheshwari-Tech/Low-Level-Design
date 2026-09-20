from dataclasses import dataclass

from app.models.cell import Cell
from app.models.enums import ShotStatus


@dataclass(frozen=True)
class Shot:
    target: Cell
    status: ShotStatus
    ship_name: str | None = None


@dataclass(frozen=True)
class ShotOutcome:
    shot: Shot
    sunk_ship_name: str | None
    won: bool
