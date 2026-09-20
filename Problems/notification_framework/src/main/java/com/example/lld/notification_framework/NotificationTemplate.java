package com.example.lld.notification_framework;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record NotificationTemplate(
        String notificationType,
        int version,
        java.util.Locale locale,
        Channel channel,
        String subjectTemplate,
        String bodyTemplate,
        Set<String> requiredVariables) {

    public NotificationTemplate {
        if (Objects.requireNonNull(notificationType, "notificationType").isBlank()) {
            throw new IllegalArgumentException("Notification type cannot be blank");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("Template version must be positive");
        }
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(subjectTemplate, "subjectTemplate");
        Objects.requireNonNull(bodyTemplate, "bodyTemplate");
        requiredVariables = Set.copyOf(
                Objects.requireNonNull(requiredVariables, "requiredVariables"));
    }

    public RenderedMessage render(Map<String, String> variables) {
        Objects.requireNonNull(variables, "variables");
        for (String required : requiredVariables) {
            if (!variables.containsKey(required) || variables.get(required) == null) {
                throw new NotificationException.TemplateRenderException(
                        "Missing required template variable: " + required);
            }
        }
        String subject = subjectTemplate;
        String body = bodyTemplate;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            subject = subject.replace(placeholder, entry.getValue());
            body = body.replace(placeholder, entry.getValue());
        }
        return new RenderedMessage(subject, body);
    }
}
