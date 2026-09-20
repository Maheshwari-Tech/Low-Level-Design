package com.example.lld.notification_framework;

/** Base type for notification domain and configuration failures. */
public class NotificationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static final class IdempotencyConflictException extends NotificationException {
        private static final long serialVersionUID = 1L;

        public IdempotencyConflictException(String message) {
            super(message);
        }
    }

    public static final class TemplateNotFoundException extends NotificationException {
        private static final long serialVersionUID = 1L;

        public TemplateNotFoundException(String message) {
            super(message);
        }
    }

    public static final class TemplateRenderException extends NotificationException {
        private static final long serialVersionUID = 1L;

        public TemplateRenderException(String message) {
            super(message);
        }
    }
}
