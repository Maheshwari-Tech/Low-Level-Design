# E-Commerce Platform

## Purpose

This folder is an umbrella for a set of focused e-commerce exercises. A complete platform coordinates catalog discovery, cart, pricing, inventory reservation, payment, order management, and fulfilment; those responsibilities should not collapse into one service class.

## Canonical components

1. [Product Catalog Service](../product_catalog_service/) owns product definitions and lifecycle.
2. [Product Search System](../product_search_system/) owns query, filters, sorting, and relevance.
3. [Shopping Cart with Expiration](../shopping_cart_with_expiration/) owns a shopper's tentative selections.
4. [Coupon / Promotion Engine](../coupon_promotion_engine/) evaluates discounts and redemption limits.
5. [Inventory Reservation Service](../inventory_reservation_service/) protects scarce stock during checkout.
6. [Payment Processing Service](../payment_processing_service/) owns authorization, capture, and refunds.
7. [Order Management System](../order_processing_system/) owns the post-checkout order lifecycle.
8. [Warehouse Fulfilment](../warehouse_fulfilment_domain/) owns physical pick, pack, and ship work.

## Checkout workflow

Create immutable product and price snapshots, validate the cart, evaluate promotions, reserve every item atomically, authorize payment, create/confirm the order, and commit or compensate each earlier step when a later operation fails. Every retried command requires an idempotency key.

## Local material

The code below `com/example/lld/e_commerce/` contains legacy exercises for priority-based order processing, product catalog search, cart expiration, and sales analytics. The standalone pages above are the canonical specifications; the embedded code is retained as implementation material and should be migrated rather than copied when extended.

## Interview focus

Define ownership boundaries, money and rounding, state machines, inventory/payment failure compensation, concurrency around stock and coupon limits, and a traceable order history. Keep taxes, shipping, payment providers, and notifications behind interfaces.
