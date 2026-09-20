package com.example.lld.warehouse_fulfilment_domain.model;

public record Destination(String addressLine, String city, String postalCode, String country) {
    public Destination {
        addressLine = requireText(addressLine, "addressLine");
        city = requireText(city, "city");
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
