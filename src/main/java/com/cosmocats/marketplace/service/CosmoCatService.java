package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.dto.CosmoCatDto;
import com.cosmocats.marketplace.dto.KittyProductDto;
import com.cosmocats.marketplace.featuretoggle.Feature;
import com.cosmocats.marketplace.featuretoggle.FeatureToggle;
import org.springframework.stereotype.Service;

import java.util.List;

/** Space cat crew and kitten goods; each method is behind its own feature flag. */
@Service
public class CosmoCatService {

    private static final List<CosmoCatDto> COSMO_CATS = List.of(
            new CosmoCatDto("Captain Whiskers", "Mars", "Navigator"),
            new CosmoCatDto("Luna Purrington", "Moon", "Engineer"),
            new CosmoCatDto("Comet Tailor", "Europa", "Pilot"));

    private static final List<KittyProductDto> KITTY_PRODUCTS = List.of(
            new KittyProductDto("Starter Star Ball", "0-6 months"),
            new KittyProductDto("Mini Moon Milk", "0-3 months"));

    @FeatureToggle(Feature.COSMO_CATS)
    public List<CosmoCatDto> getCosmoCats() {
        return COSMO_CATS;
    }

    @FeatureToggle(Feature.KITTY_PRODUCTS)
    public List<KittyProductDto> getKittyProducts() {
        return KITTY_PRODUCTS;
    }
}
