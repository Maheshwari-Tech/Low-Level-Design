package com.mycompany.app.notification;

import java.util.*;

public class EmailNotification extends Notification {
    private String email;

    public boolean sendNotification() {
        // Send Email logic here
        return true;
    }

    // Getter and Setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
