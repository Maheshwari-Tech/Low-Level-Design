from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.error_handlers import battleship_error_handler
from app.api.routes import create_router
from app.controllers.game_controller import GameController
from app.exceptions.game_exceptions import BattleshipError
from app.factories.ship_factory import ShipFactory
from app.models.board import Board
from app.models.game import BattleshipGame
from app.repositories.in_memory_game_repository import InMemoryGameRepository
from app.services.game_service import GameService


def create_app() -> FastAPI:
    application = FastAPI(title="Battleship API", version="2.0.0")
    application.add_middleware(
        CORSMiddleware,
        allow_origins=["http://127.0.0.1:5101"],
        allow_methods=["GET", "POST"],
        allow_headers=["Content-Type"],
    )

    ship_factory = ShipFactory()
    game = BattleshipGame(Board(GameService.BOARD_SIZE), ship_factory)
    repository = InMemoryGameRepository(game)
    service = GameService(repository, ship_factory)
    controller = GameController(service)
    application.include_router(create_router(controller))

    application.add_exception_handler(BattleshipError, battleship_error_handler)

    return application


app = create_app()
