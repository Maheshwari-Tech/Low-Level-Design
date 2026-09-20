from threading import Lock

from app.models.game import BattleshipGame
from app.repositories.game_repository import GameRepository


class InMemoryGameRepository(GameRepository):
    def __init__(self, game: BattleshipGame) -> None:
        self._game = game
        self._lock = Lock()

    def get(self) -> BattleshipGame:
        with self._lock:
            return self._game

    def save(self, game: BattleshipGame) -> None:
        with self._lock:
            self._game = game
