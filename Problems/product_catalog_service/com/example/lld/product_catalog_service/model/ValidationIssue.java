package com.example.lld.product_catalog_service.model;

public record ValidationIssue(String path, String message) {
    public ValidationIssue {
        path = ModelSupport.requireNonBlank(path, "path");
        message = ModelSupport.requireNonBlank(message, "message");
    }

    @Override
    public String toString() {
        return path + ": " + message;
    }
}
