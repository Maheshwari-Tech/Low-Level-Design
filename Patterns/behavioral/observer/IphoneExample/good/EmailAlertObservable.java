package code.behavioral.observer.IphoneExample.good;

import java.util.Objects;

public final class EmailAlertObservable implements NotificationAlertObservable {
    private final String user;

    public EmailAlertObservable(String user) {
        this.user = Objects.requireNonNull(user, "user");
    }

    @Override
    public void update(String product, int stock) {
        System.out.printf("email %s: %s is back in stock (%d)%n", user, product, stock);
    }
}
