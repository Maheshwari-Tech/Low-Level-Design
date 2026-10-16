from pydantic import BaseModel, Field


class HoldSeatsRequest(BaseModel):
    seats: list[str] = Field(min_length=1, max_length=6)


class ConfirmBookingRequest(BaseModel):
    hold_id: str = Field(min_length=1)
    customer_name: str = Field(min_length=2, max_length=80)
