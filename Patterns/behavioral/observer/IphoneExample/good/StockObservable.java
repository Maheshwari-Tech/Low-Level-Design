package code.behavioral.observer.IphoneExample.good;

public interface StockObservable {
    void addObserver(NotificationAlertObservable observer);

    void removeObserver(NotificationAlertObservable observer);

    void setStock(int stock);
}
