package com.cosmocats.marketplace.mapper;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.CategoryDto;
import com.cosmocats.marketplace.dto.ProductRequestDto;
import com.cosmocats.marketplace.dto.ProductResponseDto;
import org.springframework.stereotype.Component;

/**
 * Hand-written mapper between the Product domain model and its DTOs.
 * Kept as the reference implementation; the service now uses the generated
 * {@link ProductMapStructMapper}, which replaces this boilerplate.
 */
@Component
public class ProductMapper {

    public ProductResponseDto toDto(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponseDto(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                toDto(product.category()),
                product.sellerEmail());
    }

    public CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(category.id(), category.name());
    }

    /**
     * Builds a new (not yet persisted) product from the request. The category is
     * resolved by the service, because the request only carries its id.
     */
    public Product toDomain(ProductRequestDto dto, Category category) {
        if (dto == null) {
            return null;
        }
        return new Product(
                null,
                dto.name(),
                dto.description(),
                dto.price(),
                category,
                dto.sellerEmail());
    }
}
