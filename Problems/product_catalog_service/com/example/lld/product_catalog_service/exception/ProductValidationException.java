package com.example.lld.product_catalog_service.exception;

import com.example.lld.product_catalog_service.model.ProductId;
import com.example.lld.product_catalog_service.model.ValidationIssue;
import java.util.List;

public final class ProductValidationException extends CatalogException {
    private static final long serialVersionUID = 1L;

    private final transient List<ValidationIssue> issues;

    public ProductValidationException(ProductId productId, List<ValidationIssue> issues) {
        super("product " + productId + " is invalid: " + String.join(
                "; ",
                issues.stream().map(ValidationIssue::toString).toList()));
        if (issues.isEmpty()) {
            throw new IllegalArgumentException("validation exception requires at least one issue");
        }
        this.issues = List.copyOf(issues);
    }

    public List<ValidationIssue> issues() {
        return issues;
    }
}
