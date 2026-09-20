package com.example.lld.order_processing_system.model;

/** Immutable delivery-address snapshot retained with an order. */
public record Address(String line1, String city, String state, String postalCode, String country) {
    public Address {
        line1 = requireText(line1, "line1");
        city = requireText(city, "city");
        state = requireText(state, "state");
        postalCode = requireText(postalCode, "postalCode");
        country = requireText(country, "country");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
