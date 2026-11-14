package com.cosmocats.marketplace.featuretoggle;

import com.cosmocats.marketplace.service.CosmoCatService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Small Spring context (no web, no database): flags come from application.yml,
 * and Spring AOP applies {@link FeatureToggleAspect} to {@link CosmoCatService}.
 */
class FeatureToggleContextTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    @Import({FeatureToggleConfig.class, FeatureToggleService.class, FeatureToggleAspect.class, CosmoCatService.class})
    static class FeatureToggleSlice {
    }

    @Nested
    @SpringBootTest(classes = FeatureToggleSlice.class)
    class WithApplicationYml {

        @Autowired
        private CosmoCatService cosmoCatService;

        @Autowired
        private FeatureToggleService featureToggleService;

        @Test
        void flagsAreReadFromConfiguration() {
            assertThat(featureToggleService.isEnabled(Feature.COSMO_CATS)).isTrue();
            assertThat(featureToggleService.isEnabled(Feature.KITTY_PRODUCTS)).isFalse();
        }

        @Test
        void serviceIsProxiedByTheAspect() {
            assertThat(AopUtils.isAopProxy(cosmoCatService)).isTrue();
            assertThat(cosmoCatService.getCosmoCats()).isNotEmpty();
            assertThatThrownBy(cosmoCatService::getKittyProducts).isInstanceOf(FeatureNotAvailableException.class);
        }
    }

    @Nested
    @SpringBootTest(classes = FeatureToggleSlice.class, properties = {
            "feature.cosmoCats.enabled=false",
            "feature.kittyProducts.enabled=true"})
    class WithFlagsFlipped {

        @Autowired
        private CosmoCatService cosmoCatService;

        @Test
        void environmentDecidesWhichMethodsRun() {
            assertThatThrownBy(cosmoCatService::getCosmoCats).isInstanceOf(FeatureNotAvailableException.class);
            assertThat(cosmoCatService.getKittyProducts()).isNotEmpty();
        }
    }
}
