from app.models.enums import Direction
from app.models.game import BattleshipGame
from app.models.shot import ShotOutcome
from app.services.game_service import GameService


class GameController:
    def __init__(self, service: GameService) -> None:
        self._service = service

    def get_game(self) -> dict[str, object]:
        return self._serialize_game(self._service.get_game())

    def place_ship(
        self,
        name: str,
        row: int,
        column: int,
        direction: Direction,
    ) -> dict[str, object]:
        game = self._service.place_ship(name, row, column, direction)
        return self._serialize_game(game)

    def fire(self, row: int, column: int) -> dict[str, object]:
        return self._serialize_outcome(self._service.fire(row, column))

    def reset(self) -> dict[str, object]:
        return self._serialize_game(self._service.reset())

    @staticmethod
    def _serialize_game(game: BattleshipGame) -> dict[str, object]:
        return {
            "boardSize": game.board.size,
            "fleet": game.fleet_sizes,
            "status": game.status,
            "ready": game.is_ready,
            "ships": [
                {
                    "name": ship.name,
                    "size": ship.size,
                    "sunk": ship.is_sunk,
                    "cells": [
                        {"row": cell.row, "column": cell.column} for cell in ship.cells
                    ],
                }
                for ship in game.board.ships
            ],
            "shots": [
                {
                    "row": shot.target.row,
                    "column": shot.target.column,
                    "result": shot.status,
                }
                for shot in game.board.shots
            ],
        }

    @staticmethod
    def _serialize_outcome(outcome: ShotOutcome) -> dict[str, object]:
        return {
            "result": outcome.shot.status,
            "ship": outcome.shot.ship_name,
            "sunk": outcome.sunk_ship_name is not None,
            "sunkShip": outcome.sunk_ship_name,
            "won": outcome.won,
        }
