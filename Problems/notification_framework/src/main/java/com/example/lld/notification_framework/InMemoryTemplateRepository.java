package com.example.lld.notification_framework;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTemplateRepository implements TemplateRepository {
    private record TemplateKey(String type, int version, Locale locale, Channel channel) {
    }

    private final Map<TemplateKey, NotificationTemplate> templates = new ConcurrentHashMap<>();

    public void register(NotificationTemplate template) {
        Objects.requireNonNull(template, "template");
        TemplateKey key = new TemplateKey(
                template.notificationType(), template.version(), template.locale(), template.channel());
        if (templates.putIfAbsent(key, template) != null) {
            throw new IllegalArgumentException("Template already registered: " + key);
        }
    }

    @Override
    public NotificationTemplate resolve(String type, int version, Locale locale, Channel channel) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(channel, "channel");
        Set<Locale> candidates = new LinkedHashSet<>();
        candidates.add(locale);
        if (!locale.getLanguage().isBlank()) {
            candidates.add(Locale.forLanguageTag(locale.getLanguage()));
        }
        candidates.add(Locale.ENGLISH);
        for (Locale candidate : candidates) {
            NotificationTemplate template = templates.get(
                    new TemplateKey(type, version, candidate, channel));
            if (template != null) {
                return template;
            }
        }
        throw new NotificationException.TemplateNotFoundException(
                "No template for " + type + " v" + version + ", " + locale + ", " + channel);
    }
}
