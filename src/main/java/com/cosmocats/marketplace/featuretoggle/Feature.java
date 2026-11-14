package com.cosmocats.marketplace.featuretoggle;

/** Features that can be switched on and off with {@code feature.<key>.enabled}. */
public enum Feature {
    COSMO_CATS("cosmoCats"),
    KITTY_PRODUCTS("kittyProducts");

    private final String key;

    Feature(String key) {
        this.key = key;
    }

    /** Name of the feature in the configuration, e.g. {@code cosmoCats}. */
    public String key() {
        return key;
    }
}
