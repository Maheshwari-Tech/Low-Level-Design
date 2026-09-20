package com.example.lld.notification_framework;

public interface ChannelProvider {
    String name();

    Channel channel();

    ProviderResponse send(ProviderMessage message) throws ProviderFailureException;
}
