package com.cosmocats.marketplace.featuretoggle;

public class FeatureNotAvailableException extends RuntimeException {

    private final transient Feature feature;

    public FeatureNotAvailableException(Feature feature) {
        super("Feature " + feature.key() + " is not available");
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }
}
