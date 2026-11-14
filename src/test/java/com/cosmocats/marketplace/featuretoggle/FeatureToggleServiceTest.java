package com.cosmocats.marketplace.featuretoggle;

import com.cosmocats.marketplace.featuretoggle.FeatureToggleProperties.FeatureSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureToggleServiceTest {

    private final FeatureToggleService service = new FeatureToggleService(new FeatureToggleProperties(Map.of(
            "cosmoCats", new FeatureSettings(true),
            "kittyProducts", new FeatureSettings(false))));

    @Test
    void readsFlagsByFeature() {
        assertThat(service.isEnabled(Feature.COSMO_CATS)).isTrue();
        assertThat(service.isEnabled(Feature.KITTY_PRODUCTS)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cosmoCats", "cosmo-cats", "COSMO_CATS", "cosmocats"})
    void acceptsEverySpellingOfTheName(String name) {
        assertThat(service.isEnabled(name)).isTrue();
    }

    @Test
    void unknownFeatureIsDisabled() {
        assertThat(service.isEnabled("teleport")).isFalse();
    }

    @Test
    void noConfigurationMeansEverythingIsDisabled() {
        FeatureToggleService empty = new FeatureToggleService(new FeatureToggleProperties(null));

        assertThat(empty.isEnabled(Feature.COSMO_CATS)).isFalse();
    }
}
