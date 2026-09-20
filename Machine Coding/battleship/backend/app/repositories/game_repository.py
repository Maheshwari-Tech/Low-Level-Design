from abc import ABC, abstractmethod

from app.models.game import BattleshipGame


class GameRepository(ABC):
    @abstractmethod
    def get(self) -> BattleshipGame:
        pass

    @abstractmethod
    def save(self, game: BattleshipGame) -> None:
        pass
