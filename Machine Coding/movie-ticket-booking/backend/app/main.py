from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .api import create_router
from .application import BookingService
from .infrastructure import InMemoryBookingRepository


def create_app() -> FastAPI:
    app = FastAPI(title="Movie Ticket Booking API", version="1.0.0")
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://127.0.0.1:5102"],
        allow_methods=["*"],
        allow_headers=["*"],
    )
    service = BookingService(InMemoryBookingRepository())
    app.include_router(create_router(service))
    return app


app = create_app()
