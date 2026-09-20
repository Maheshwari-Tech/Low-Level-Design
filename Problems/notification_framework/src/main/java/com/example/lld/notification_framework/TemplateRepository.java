package com.example.lld.notification_framework;

import java.util.Locale;

public interface TemplateRepository {
    NotificationTemplate resolve(String type, int version, Locale locale, Channel channel);
}
