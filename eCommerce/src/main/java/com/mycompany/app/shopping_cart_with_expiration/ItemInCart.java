package com.mycompany.app.shopping_cart_with_expiration;

import java.time.LocalDateTime;

public record ItemInCart(Item item, LocalDateTime expirationTime) {


}
