from app.factories.ship_factory import ShipFactory
from app.models.board import Board
from app.models.cell import Cell
from app.models.enums import Direction
from app.models.game import BattleshipGame
from app.models.shot import ShotOutcome
from app.repositories.game_repository import GameRepository


class GameService:
    BOARD_SIZE = 8

    def __init__(self, repository: GameRepository, ship_factory: ShipFactory) -> None:
        self._repository = repository
        self._ship_factory = ship_factory

    def get_game(self) -> BattleshipGame:
        return self._repository.get()

    def place_ship(
        self,
        name: str,
        row: int,
        column: int,
        direction: Direction,
    ) -> BattleshipGame:
        game = self._repository.get()
        game.place_ship(name, Cell(row, column), direction)
        return game

    def fire(self, row: int, column: int) -> ShotOutcome:
        return self._repository.get().fire(Cell(row, column))

    def reset(self) -> BattleshipGame:
        game = BattleshipGame(Board(self.BOARD_SIZE), self._ship_factory)
        self._repository.save(game)
        return game
