package com.mycompany.app.notification;

import java.util.*;

public class SmsNotification extends Notification {
    private String phoneNumber;

    public boolean sendNotification() {
        // Send SMS logic here
        return true;
    }

    // Getter and Setter
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
