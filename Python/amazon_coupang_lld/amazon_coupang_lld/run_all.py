from . import (
    inventory_reservation,
    notification_framework,
    order_management,
    payment_processing,
    promotion_engine,
    rate_limiter,
    warehouse_fulfilment,
)


def main() -> None:
    order_management.run_demo()
    inventory_reservation.run_demo()
    promotion_engine.run_demo()
    notification_framework.run_demo()
    payment_processing.run_demo()
    warehouse_fulfilment.run_demo()
    rate_limiter.run_demo()
    print("All Python Amazon/Coupang LLD demos passed")


if __name__ == "__main__":
    main()
