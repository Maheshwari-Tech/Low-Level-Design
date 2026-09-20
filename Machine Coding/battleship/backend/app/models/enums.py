from enum import StrEnum


class Direction(StrEnum):
    HORIZONTAL = "horizontal"
    VERTICAL = "vertical"


class ShotStatus(StrEnum):
    HIT = "hit"
    MISS = "miss"


class GameStatus(StrEnum):
    PLACING_FLEET = "placing_fleet"
    READY = "ready"
    IN_PROGRESS = "in_progress"
    WON = "won"
