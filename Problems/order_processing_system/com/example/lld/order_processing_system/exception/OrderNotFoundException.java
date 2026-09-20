package com.example.lld.order_processing_system.exception;

public final class OrderNotFoundException extends OrderDomainException {
    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
