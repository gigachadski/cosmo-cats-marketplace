package com.cosmocats.marketplace.featuretoggle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated method runs only when the feature is enabled; otherwise
 * {@link FeatureToggleAspect} throws {@link FeatureNotAvailableException}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureToggle {

    Feature value();
}
