from datetime import datetime

from pydantic import BaseModel, Field


class CreateMeetingRequest(BaseModel):
    title: str = Field(min_length=2, max_length=100)
    participants: list[str] = Field(min_length=1)
    start: datetime
    end: datetime
