from pydantic import BaseModel, Field

from app.models.enums import Direction


class PlaceShipRequest(BaseModel):
    name: str = Field(min_length=1)
    row: int = Field(ge=0)
    column: int = Field(ge=0)
    direction: Direction


class FireRequest(BaseModel):
    row: int = Field(ge=0)
    column: int = Field(ge=0)
