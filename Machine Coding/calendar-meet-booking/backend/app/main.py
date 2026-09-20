from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .api import create_router
from .application import CalendarService
from .infrastructure import seeded_repository


def create_app() -> FastAPI:
    app = FastAPI(title="Calendar Meet Booking API", version="1.0.0")
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://127.0.0.1:5103"],
        allow_methods=["*"],
        allow_headers=["*"],
    )
    app.include_router(create_router(CalendarService(seeded_repository())))
    return app


app = create_app()
