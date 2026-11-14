package com.cosmocats.marketplace.featuretoggle;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Gives the application access to the map of feature flags from the configuration. */
@Configuration
@EnableConfigurationProperties(FeatureToggleProperties.class)
public class FeatureToggleConfig {
}
