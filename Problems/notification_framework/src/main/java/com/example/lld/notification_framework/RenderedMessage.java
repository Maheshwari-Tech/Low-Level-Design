package com.example.lld.notification_framework;

import java.util.Objects;

public record RenderedMessage(String subject, String body) {
    public RenderedMessage {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(body, "body");
    }
}
