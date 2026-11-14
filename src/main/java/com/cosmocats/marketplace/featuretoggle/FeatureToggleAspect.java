package com.cosmocats.marketplace.featuretoggle;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Checks the feature flag before every method annotated with {@link FeatureToggle}. */
@Aspect
@Component
public class FeatureToggleAspect {

    private static final Logger log = LoggerFactory.getLogger(FeatureToggleAspect.class);

    private final FeatureToggleService featureToggleService;

    public FeatureToggleAspect(FeatureToggleService featureToggleService) {
        this.featureToggleService = featureToggleService;
    }

    @Around("@annotation(featureToggle)")
    public Object checkFeature(ProceedingJoinPoint joinPoint, FeatureToggle featureToggle) throws Throwable {
        Feature feature = featureToggle.value();
        if (!featureToggleService.isEnabled(feature)) {
            log.debug("Feature {} is disabled, {} was not executed", feature.key(), joinPoint.getSignature().toShortString());
            throw new FeatureNotAvailableException(feature);
        }
        return joinPoint.proceed();
    }
}
