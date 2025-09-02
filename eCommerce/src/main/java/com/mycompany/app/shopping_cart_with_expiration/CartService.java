package com.mycompany.app.shopping_cart_with_expiration;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {

    void add(Item item, long durationInMinute);
    BigDecimal getPrice();
    List<ItemInCart> getItems();

}
