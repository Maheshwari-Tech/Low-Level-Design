package com.mycompany.app.shopping_cart_with_expiration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class CartServiceImpl implements CartService{
    BigDecimal totalPrice = BigDecimal.valueOf(0);

    PriorityQueue<ItemInCart> pq = new PriorityQueue<>((a, b) -> a.expirationTime().compareTo(b.expirationTime()));

    @Override
    public void add(Item item, long durationInMinute) {
        pq.add(new ItemInCart(item, LocalDateTime.now().plusMinutes(durationInMinute)));
        totalPrice = totalPrice.add(item.getPrice());
    }

    @Override
    public BigDecimal getPrice() {
       expireLazy();
       return totalPrice;
    }

    private void expireLazy(){
        while(!pq.isEmpty() && pq.peek().expirationTime().isBefore(LocalDateTime.now())){
            assert pq.peek() != null;
            ItemInCart expired = pq.poll();
            totalPrice = totalPrice.subtract(expired.item().getPrice());
            pq.poll();
        }
    }

    @Override
    public List<ItemInCart> getItems() {
        expireLazy();
        return new ArrayList<>(pq);
    }
}
