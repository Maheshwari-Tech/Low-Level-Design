from dataclasses import dataclass


@dataclass(frozen=True, order=True)
class Cell:
    row: int
    column: int
