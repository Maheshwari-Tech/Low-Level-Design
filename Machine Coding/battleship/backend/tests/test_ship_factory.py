import pytest

from app.factories.ship_factory import ShipFactory


def test_rejects_empty_fleet_configuration():
    with pytest.raises(ValueError, match="at least one ship"):
        ShipFactory({})


@pytest.mark.parametrize(
    "fleet",
    [
        {"": 2},
        {"Destroyer": 0},
        {"Destroyer": 2, "destroyer": 3},
    ],
)
def test_rejects_invalid_fleet_configuration(fleet):
    with pytest.raises(ValueError):
        ShipFactory(fleet)
