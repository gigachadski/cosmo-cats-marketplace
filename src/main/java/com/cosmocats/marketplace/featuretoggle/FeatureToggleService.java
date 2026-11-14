package com.cosmocats.marketplace.featuretoggle;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Answers whether a feature is switched on. A feature missing from the configuration is off. */
@Service
public class FeatureToggleService {

    private final Map<String, Boolean> flags = new HashMap<>();

    public FeatureToggleService(FeatureToggleProperties properties) {
        properties.feature().forEach((name, settings) -> flags.put(normalize(name), settings.enabled()));
    }

    public boolean isEnabled(Feature feature) {
        return isEnabled(feature.key());
    }

    /** Accepts any spelling Spring accepts in configuration: cosmoCats, cosmo-cats, COSMO_CATS. */
    public boolean isEnabled(String featureName) {
        return flags.getOrDefault(normalize(featureName), false);
    }

    private static String normalize(String name) {
        return name.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
    }
}
