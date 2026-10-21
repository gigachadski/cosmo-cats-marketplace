package com.cosmocats.marketplace.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CosmicWordCheckValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    record Named(@CosmicWordCheck String name) {
    }

    record CustomWords(@CosmicWordCheck(words = {"Pulsar"}) String name) {
    }

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Star Tuna", "GALAXY yarn", "Comet post", "Starlight Bed", "Nebula catnip", "space milk"})
    void acceptsNamesWithCosmicWords(String name) {
        assertThat(validator.validate(new Named(name))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tuna Can", "Yarn Ball", "Scratching Post", ""})
    void rejectsNamesWithoutCosmicWords(String name) {
        assertThat(validator.validate(new Named(name)))
                .singleElement()
                .satisfies(violation -> assertThat(violation.getMessage())
                        .isEqualTo("must contain a cosmic word (e.g. star, galaxy, comet)"));
    }

    @Test
    void treatsNullAsValid() {
        assertThat(validator.validate(new Named(null))).isEmpty();
    }

    @Test
    void customWordListIsCaseInsensitive() {
        assertThat(validator.validate(new CustomWords("PULSAR collar"))).isEmpty();
        assertThat(validator.validate(new CustomWords("Star collar"))).hasSize(1);
    }
}
