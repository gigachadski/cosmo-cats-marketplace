package com.cosmocats.marketplace.featuretoggle;

import com.cosmocats.marketplace.dto.CosmoCatDto;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleProperties.FeatureSettings;
import com.cosmocats.marketplace.service.CosmoCatService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests: the real aspect is woven around the service with a proxy, without a Spring context. */
class CosmoCatServiceTest {

    private static CosmoCatService serviceWithFlags(Map<String, FeatureSettings> flags) {
        FeatureToggleService toggles = new FeatureToggleService(new FeatureToggleProperties(flags));
        AspectJProxyFactory factory = new AspectJProxyFactory(new CosmoCatService());
        factory.addAspect(new FeatureToggleAspect(toggles));
        return factory.getProxy();
    }

    @Test
    void getCosmoCatsRunsWhenFlagIsEnabled() {
        CosmoCatService service = serviceWithFlags(Map.of("cosmoCats", new FeatureSettings(true)));

        assertThat(service.getCosmoCats())
                .hasSize(3)
                .extracting(CosmoCatDto::name)
                .contains("Captain Whiskers");
    }

    @Test
    void getCosmoCatsThrowsWhenFlagIsDisabled() {
        CosmoCatService service = serviceWithFlags(Map.of("cosmoCats", new FeatureSettings(false)));

        assertThatThrownBy(service::getCosmoCats)
                .isInstanceOf(FeatureNotAvailableException.class)
                .hasMessage("Feature cosmoCats is not available")
                .extracting(ex -> ((FeatureNotAvailableException) ex).getFeature())
                .isEqualTo(Feature.COSMO_CATS);
    }

    @Test
    void getCosmoCatsThrowsWhenFlagIsMissing() {
        CosmoCatService service = serviceWithFlags(Map.of());

        assertThatThrownBy(service::getCosmoCats).isInstanceOf(FeatureNotAvailableException.class);
    }

    @Test
    void eachMethodDependsOnlyOnItsOwnFlag() {
        CosmoCatService service = serviceWithFlags(Map.of(
                "cosmoCats", new FeatureSettings(true),
                "kittyProducts", new FeatureSettings(false)));

        assertThat(service.getCosmoCats()).isNotEmpty();
        assertThatThrownBy(service::getKittyProducts)
                .isInstanceOf(FeatureNotAvailableException.class)
                .hasMessage("Feature kittyProducts is not available");
    }

    @Test
    void kittyProductsRunWhenTheirFlagIsEnabled() {
        CosmoCatService service = serviceWithFlags(Map.of("kittyProducts", new FeatureSettings(true)));

        assertThat(service.getKittyProducts()).hasSize(2);
        assertThatThrownBy(service::getCosmoCats).isInstanceOf(FeatureNotAvailableException.class);
    }
}
