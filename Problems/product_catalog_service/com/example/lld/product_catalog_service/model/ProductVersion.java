package com.example.lld.product_catalog_service.model;

import java.time.Instant;
import java.util.Objects;

public record ProductVersion(
        long version,
        ProductChangeType changeType,
        String actor,
        Instant recordedAt,
        Product snapshot) {

    public ProductVersion {
        if (version < 1) {
            throw new IllegalArgumentException("version must be at least one");
        }
        changeType = Objects.requireNonNull(changeType, "changeType must not be null");
        actor = ModelSupport.requireNonBlank(actor, "actor");
        recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        snapshot = Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (snapshot.version() != version) {
            throw new IllegalArgumentException("history version must match snapshot version");
        }
    }
}
