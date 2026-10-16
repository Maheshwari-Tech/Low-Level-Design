from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.error_handlers import booking_error_handler
from app.api.routes import create_router
from app.controllers.booking_controller import BookingController
from app.exceptions.booking_exceptions import BookingError
from app.repositories.in_memory_booking_repository import InMemoryBookingRepository
from app.services.booking_service import BookingService


def create_app() -> FastAPI:
    application = FastAPI(title="Movie Ticket Booking API", version="1.0.0")
    application.add_middleware(
        CORSMiddleware,
        allow_origins=["http://127.0.0.1:5102"],
        allow_methods=["GET", "POST"],
        allow_headers=["Content-Type"],
    )
    service = BookingService(InMemoryBookingRepository())
    application.include_router(create_router(BookingController(service)))
    application.add_exception_handler(BookingError, booking_error_handler)
    return application


app = create_app()
