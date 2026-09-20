from app.models.cell import Cell


class Ship:
    def __init__(self, name: str, size: int) -> None:
        if size < 1:
            raise ValueError("Ship size must be positive")
        self._name = name
        self._size = size
        self._cells: tuple[Cell, ...] = ()
        self._hits: set[Cell] = set()

    @property
    def name(self) -> str:
        return self._name

    @property
    def size(self) -> int:
        return self._size

    @property
    def cells(self) -> tuple[Cell, ...]:
        return self._cells

    @property
    def hits(self) -> frozenset[Cell]:
        return frozenset(self._hits)

    @property
    def is_sunk(self) -> bool:
        return bool(self._cells) and len(self._hits) == self._size

    def place_at(self, cells: tuple[Cell, ...]) -> None:
        if self._cells:
            raise ValueError("Ship is already placed")
        if len(cells) != self._size:
            raise ValueError("Placement must contain one cell per ship segment")
        self._cells = cells

    def occupies(self, cell: Cell) -> bool:
        return cell in self._cells

    def register_hit(self, cell: Cell) -> None:
        if not self.occupies(cell):
            raise ValueError("Cannot hit a cell that the ship does not occupy")
        self._hits.add(cell)
