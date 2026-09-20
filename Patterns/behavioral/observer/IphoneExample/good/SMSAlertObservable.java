package code.behavioral.observer.IphoneExample.good;

import java.util.Objects;

public final class SMSAlertObservable implements NotificationAlertObservable {
    private final String phoneNumber;

    public SMSAlertObservable(String phoneNumber) {
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
    }

    @Override
    public void update(String product, int stock) {
        System.out.printf("sms %s: %s is back in stock (%d)%n", phoneNumber, product, stock);
    }
}
