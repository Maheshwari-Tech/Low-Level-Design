package com.example.lld.product_catalog_service.model;

import java.net.URI;
import java.util.Objects;

public record MediaAsset(URI uri, String altText, int sortOrder) {
    public MediaAsset {
        uri = Objects.requireNonNull(uri, "uri must not be null");
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("media URI must be absolute");
        }
        altText = ModelSupport.requireNonBlank(altText, "altText");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be zero or greater");
        }
    }
}
