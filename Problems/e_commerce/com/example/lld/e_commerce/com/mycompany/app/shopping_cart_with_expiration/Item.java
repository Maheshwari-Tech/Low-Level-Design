package com.mycompany.app.shopping_cart_with_expiration;

import java.math.BigDecimal;

public class Item {
    private final String name;
    private final int id;
    private final BigDecimal price;

    public Item(String name, int id, BigDecimal price) {
        this.name = name;
        this.id = id;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
