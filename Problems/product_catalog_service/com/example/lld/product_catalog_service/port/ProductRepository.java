package com.example.lld.product_catalog_service.port;

import com.example.lld.product_catalog_service.model.Product;
import com.example.lld.product_catalog_service.model.ProductId;
import com.example.lld.product_catalog_service.model.ProductVersion;
import com.example.lld.product_catalog_service.model.Sku;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product insert(Product product, ProductVersion initialVersion);

    Product replace(Product product, long expectedVersion, ProductVersion newVersion);

    Optional<Product> findById(ProductId productId);

    Optional<Product> findBySku(Sku sku);

    List<Product> findAll();

    List<ProductVersion> history(ProductId productId);
}
