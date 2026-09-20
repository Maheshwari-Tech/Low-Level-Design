import pytest

from app.exceptions.game_exceptions import (
    DuplicateShotError,
    FleetIncompleteError,
    GameAlreadyStartedError,
    GameFinishedError,
    InvalidPlacementError,
    InvalidShotError,
    ShipAlreadyPlacedError,
    UnknownShipError,
)
from app.factories.ship_factory import ShipFactory
from app.models.board import Board
from app.models.enums import Direction, GameStatus, ShotStatus
from app.models.game import BattleshipGame
from app.repositories.in_memory_game_repository import InMemoryGameRepository
from app.services.game_service import GameService


def create_service() -> GameService:
    factory = ShipFactory()
    game = BattleshipGame(Board(GameService.BOARD_SIZE), factory)
    return GameService(InMemoryGameRepository(game), factory)


def place_complete_fleet(service: GameService) -> None:
    service.place_ship("Destroyer", 0, 0, Direction.HORIZONTAL)
    service.place_ship("Cruiser", 1, 0, Direction.HORIZONTAL)
    service.place_ship("Battleship", 2, 0, Direction.HORIZONTAL)


def test_places_complete_fleet_and_reports_ready():
    service = create_service()
    place_complete_fleet(service)
    assert service.get_game().is_ready is True


def test_places_ship_vertically():
    service = create_service()

    service.place_ship("Destroyer", 3, 4, Direction.VERTICAL)

    [ship] = service.get_game().board.ships
    assert [(cell.row, cell.column) for cell in ship.cells] == [(3, 4), (4, 4)]


def test_rejects_ship_outside_board():
    service = create_service()
    with pytest.raises(InvalidPlacementError):
        service.place_ship("Battleship", 0, 6, Direction.HORIZONTAL)


def test_rejects_overlapping_or_duplicate_ship():
    service = create_service()
    service.place_ship("Destroyer", 0, 0, Direction.HORIZONTAL)
    with pytest.raises(InvalidPlacementError):
        service.place_ship("Cruiser", 0, 1, Direction.VERTICAL)
    with pytest.raises(ShipAlreadyPlacedError):
        service.place_ship("Destroyer", 4, 0, Direction.HORIZONTAL)


def test_rejects_unknown_ship():
    service = create_service()

    with pytest.raises(UnknownShipError):
        service.place_ship("Submarine", 0, 0, Direction.HORIZONTAL)


def test_requires_complete_fleet_before_firing():
    service = create_service()

    with pytest.raises(FleetIncompleteError):
        service.fire(0, 0)


def test_reports_sunk_and_won():
    service = create_service()
    place_complete_fleet(service)
    service.fire(0, 0)
    destroyer_result = service.fire(0, 1)
    assert destroyer_result.sunk_ship_name == "Destroyer"

    for row, length in [(1, 3), (2, 4)]:
        for column in range(length):
            final_result = service.fire(row, column)
    assert final_result.won is True
    assert service.get_game().status is GameStatus.WON


def test_reports_miss_and_prevents_placement_after_game_starts():
    service = create_service()
    place_complete_fleet(service)

    outcome = service.fire(7, 7)

    assert outcome.shot.status is ShotStatus.MISS
    assert service.get_game().status is GameStatus.IN_PROGRESS
    with pytest.raises(GameAlreadyStartedError):
        service.place_ship("Destroyer", 4, 0, Direction.HORIZONTAL)


def test_rejects_duplicate_shot():
    service = create_service()
    place_complete_fleet(service)
    service.fire(7, 7)
    with pytest.raises(DuplicateShotError):
        service.fire(7, 7)


def test_invalid_shot_does_not_start_the_game():
    service = create_service()
    place_complete_fleet(service)
    with pytest.raises(InvalidShotError):
        service.fire(8, 0)
    assert service.get_game().status is GameStatus.READY


def test_rejects_shots_after_game_is_won():
    service = create_service()
    place_complete_fleet(service)
    for row, length in [(0, 2), (1, 3), (2, 4)]:
        for column in range(length):
            service.fire(row, column)

    with pytest.raises(GameFinishedError):
        service.fire(7, 7)


def test_reset_replaces_all_game_state():
    service = create_service()
    service.place_ship("Destroyer", 0, 0, Direction.HORIZONTAL)

    reset_game = service.reset()

    assert reset_game.status is GameStatus.PLACING_FLEET
    assert reset_game.board.ships == ()
    assert reset_game.board.shots == ()


def test_ship_names_are_case_insensitive_but_canonicalized():
    service = create_service()

    service.place_ship("destroyer", 0, 0, Direction.HORIZONTAL)

    [ship] = service.get_game().board.ships
    assert ship.name == "Destroyer"
