from fastapi import APIRouter

from app.api.schemas import FireRequest, PlaceShipRequest
from app.controllers.game_controller import GameController


def create_router(controller: GameController) -> APIRouter:
    router = APIRouter(prefix="/api/game", tags=["game"])

    @router.get("")
    def get_game() -> dict[str, object]:
        return controller.get_game()

    @router.post("/ships", status_code=201)
    def place_ship(request: PlaceShipRequest) -> dict[str, object]:
        return controller.place_ship(
            request.name,
            request.row,
            request.column,
            request.direction,
        )

    @router.post("/shots")
    def fire(request: FireRequest) -> dict[str, object]:
        return controller.fire(request.row, request.column)

    @router.post("/reset")
    def reset() -> dict[str, object]:
        return controller.reset()

    return router
