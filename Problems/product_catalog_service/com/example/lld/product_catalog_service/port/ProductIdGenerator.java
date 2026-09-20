package com.example.lld.product_catalog_service.port;

import com.example.lld.product_catalog_service.model.ProductId;

@FunctionalInterface
public interface ProductIdGenerator {
    ProductId nextProductId();
}
