package com.cosmocats.marketplace.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A cosmic word must start a word of the value: "Starlight" and "Galaxy-Yarn" pass,
 * but "Mustard" (contains "star") and "Workspace" (contains "space") do not.
 */
public class CosmicWordCheckValidator implements ConstraintValidator<CosmicWordCheck, String> {

    private Pattern cosmicWordAtWordStart;

    @Override
    public void initialize(CosmicWordCheck annotation) {
        String alternatives = Arrays.stream(annotation.words())
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        cosmicWordAtWordStart = Pattern.compile("\\b(?:" + alternatives + ")",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return cosmicWordAtWordStart.matcher(value).find();
    }
}
