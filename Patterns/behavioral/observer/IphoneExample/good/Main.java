package code.behavioral.observer.IphoneExample.good;

public class Main {

    public static void main(String[] args) {
        StockObservable iphone = new IPhoneObservableImpl("iPhone");
        NotificationAlertObservable email = new EmailAlertObservable("buyer@example.test");
        NotificationAlertObservable sms = new SMSAlertObservable("+1-555-0100");

        iphone.addObserver(email);
        iphone.addObserver(sms);
        iphone.setStock(10);

        iphone.setStock(0);
        iphone.removeObserver(sms);
        iphone.setStock(5);
    }
}
