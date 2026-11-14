package com.cosmocats.marketplace.featuretoggle;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Binds every {@code feature.<name>.enabled} entry of the configuration into one map,
 * e.g. {@code feature.cosmoCats.enabled=true} becomes {@code "cosmoCats" -> enabled=true}.
 */
@ConfigurationProperties
public record FeatureToggleProperties(Map<String, FeatureSettings> feature) {

    public FeatureToggleProperties {
        feature = feature == null ? Map.of() : Map.copyOf(feature);
    }

    public record FeatureSettings(boolean enabled) {
    }
}
