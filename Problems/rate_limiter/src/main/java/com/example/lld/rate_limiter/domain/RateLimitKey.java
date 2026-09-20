package com.example.lld.rate_limiter.domain;

import java.util.Objects;

/** A normalised namespace/value pair, suitable for user, IP, tenant, or composite keys. */
public record RateLimitKey(String namespace, String value) {
    public RateLimitKey {
        namespace = requireText(namespace, "namespace").trim().toLowerCase();
        value = requireText(value, "value").trim();
    }

    public static RateLimitKey of(String namespace, String value) {
        return new RateLimitKey(namespace, value);
    }

    public static RateLimitKey composite(String namespace, String... parts) {
        Objects.requireNonNull(parts, "parts");
        if (parts.length == 0) {
            throw new IllegalArgumentException("A composite key needs at least one part");
        }
        StringBuilder value = new StringBuilder();
        for (String part : parts) {
            String checked = requireText(part, "key part");
            if (!value.isEmpty()) {
                value.append(':');
            }
            value.append(checked.length()).append('#').append(checked);
        }
        return new RateLimitKey(namespace, value.toString());
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}
