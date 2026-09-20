package com.example.lld.payment_processing_service.exception;

public final class InvalidMoneyException extends PaymentDomainException {
    private static final long serialVersionUID = 1L;

    public InvalidMoneyException(String message) {
        super(message);
    }

    public InvalidMoneyException(String message, Throwable cause) {
        super(message, cause);
    }
}
