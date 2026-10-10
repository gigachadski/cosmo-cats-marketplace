package com.cosmocats.marketplace.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CosmicWordCheckValidator implements ConstraintValidator<CosmicWordCheck, String> {

    private List<String> cosmicWords;

    @Override
    public void initialize(CosmicWordCheck annotation) {
        cosmicWords = Arrays.stream(annotation.words())
                .map(word -> word.toLowerCase(Locale.ROOT))
                .toList();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return cosmicWords.stream().anyMatch(normalized::contains);
    }
}
