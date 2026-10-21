package com.cosmocats.marketplace.mapper;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.CategoryDto;
import com.cosmocats.marketplace.dto.ProductRequestDto;
import com.cosmocats.marketplace.dto.ProductResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct equivalent of {@link ProductMapper}: the implementation is generated
 * at compile time, so there is no reflection cost and no hand-written boilerplate.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapStructMapper {

    ProductResponseDto toDto(Product product);

    CategoryDto toDto(Category category);

    @Mapping(target = "id", ignore = true)
    // Product.withId(..) looks like a fluent setter to MapStruct; it is a domain method, not a property
    @Mapping(target = "withId", ignore = true)
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "category", source = "category")
    Product toDomain(ProductRequestDto dto, Category category);
}
