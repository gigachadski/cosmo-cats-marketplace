package com.cosmocats.marketplace.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated string must contain at least one cosmic word (case-insensitive),
 * e.g. "Star Tuna" or "Galaxy Yarn". {@code null} is considered valid; combine
 * with {@code @NotNull} to make the value mandatory.
 */
@Documented
@Constraint(validatedBy = CosmicWordCheckValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CosmicWordCheck {

    String message() default "must contain a cosmic word (e.g. star, galaxy, comet)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] words() default {
            "star", "galaxy", "comet", "planet", "nebula", "cosmic", "cosmo", "moon",
            "orbit", "asteroid", "meteor", "rocket", "space", "universe", "solar", "lunar", "astro"
    };
}
