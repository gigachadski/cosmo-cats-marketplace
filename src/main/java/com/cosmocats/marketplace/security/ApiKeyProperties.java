package com.cosmocats.marketplace.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/** API keys of machine clients: client name -> key ({@code cosmo.security.api-keys.<name>}). */
@ConfigurationProperties(prefix = "cosmo.security")
public record ApiKeyProperties(Map<String, String> apiKeys) {

    public ApiKeyProperties {
        apiKeys = apiKeys == null ? Map.of() : Map.copyOf(apiKeys);
    }
}
