package com.cosmocats.marketplace.mapper;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.CategoryDto;
import com.cosmocats.marketplace.dto.ProductRequestDto;
import com.cosmocats.marketplace.dto.ProductResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper manualMapper = new ProductMapper();
    private final ProductMapStructMapper mapStructMapper = Mappers.getMapper(ProductMapStructMapper.class);

    private final Category category = new Category(1L, "Space Food");
    private final Product product = new Product(7L, "Star Tuna Can", "Tuna from Sirius",
            new BigDecimal("9.99"), category, "tuna@cosmocats.space");
    private final ProductRequestDto request = new ProductRequestDto("Comet Treats", "Crunchy",
            new BigDecimal("3.50"), 1L, "treats@cosmocats.space");

    @Test
    void toDtoMapsAllFields() {
        assertThat(manualMapper.toDto(product)).isEqualTo(new ProductResponseDto(7L, "Star Tuna Can",
                "Tuna from Sirius", new BigDecimal("9.99"), new CategoryDto(1L, "Space Food"), "tuna@cosmocats.space"));
    }

    @Test
    void toDomainCreatesUnsavedProductWithResolvedCategory() {
        assertThat(manualMapper.toDomain(request, category)).isEqualTo(new Product(null, "Comet Treats", "Crunchy",
                new BigDecimal("3.50"), category, "treats@cosmocats.space"));
    }

    @Test
    void mapStructMapperProducesSameResultAsManualMapper() {
        assertThat(mapStructMapper.toDto(product)).isEqualTo(manualMapper.toDto(product));
        assertThat(mapStructMapper.toDto(category)).isEqualTo(manualMapper.toDto(category));
        assertThat(mapStructMapper.toDomain(request, category)).isEqualTo(manualMapper.toDomain(request, category));
    }

    @Test
    void nullsMapToNull() {
        assertThat(manualMapper.toDto((Product) null)).isNull();
        assertThat(manualMapper.toDto((Category) null)).isNull();
        assertThat(manualMapper.toDomain(null, category)).isNull();
        assertThat(mapStructMapper.toDto((Product) null)).isNull();
        assertThat(mapStructMapper.toDto((Category) null)).isNull();
        assertThat(mapStructMapper.toDomain(null, null)).isNull();
    }
}
