package code.behavioral.observer.IphoneExample.good;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class IPhoneObservableImpl implements StockObservable {
    private final Set<NotificationAlertObservable> observers = new LinkedHashSet<>();
    private final String product;
    private int stock;

    public IPhoneObservableImpl(String product) {
        this.product = Objects.requireNonNull(product, "product");
    }

    @Override
    public void addObserver(NotificationAlertObservable observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    @Override
    public void removeObserver(NotificationAlertObservable observer) {
        observers.remove(observer);
    }

    @Override
    public void setStock(int newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("stock cannot be negative");
        }
        boolean becameAvailable = stock == 0 && newStock > 0;
        stock = newStock;
        if (becameAvailable) {
            observers.forEach(observer -> observer.update(product, stock));
        }
    }
}
