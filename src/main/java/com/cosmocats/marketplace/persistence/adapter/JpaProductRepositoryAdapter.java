package com.cosmocats.marketplace.persistence.adapter;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.ProductSummaryDto;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.persistence.repository.CategoryJpaRepository;
import com.cosmocats.marketplace.persistence.repository.ProductJpaRepository;
import com.cosmocats.marketplace.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository products;
    private final CategoryJpaRepository categories;

    public JpaProductRepositoryAdapter(ProductJpaRepository products, CategoryJpaRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Override
    public Product save(Product product) {
        if (product.id() != null) {
            throw new IllegalArgumentException("A new product must not have an id");
        }
        ProductEntity entity = new ProductEntity(product.name(), product.description(), product.price(),
                categoryReference(product), product.sellerEmail());
        // flush now, so a unique-constraint violation surfaces here and not at commit time
        return EntityMapper.toDomain(products.saveAndFlush(entity));
    }

    @Override
    public Optional<Product> findById(Long id) {
        return products.findWithCategoryById(id).map(EntityMapper::toDomain);
    }

    @Override
    public List<Product> findPage(int page, int size) {
        return products.findAll(PageRequest.of(page, size, Sort.by("id"))).stream()
                .map(EntityMapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return products.count();
    }

    @Override
    public boolean existsById(Long id) {
        return products.existsById(id);
    }

    @Override
    public Optional<Product> update(Product product) {
        return products.findWithCategoryById(product.id()).map(entity -> {
            entity.replaceWith(product.name(), product.description(), product.price(),
                    categoryReference(product), product.sellerEmail());
            products.flush();
            return EntityMapper.toDomain(entity);
        });
    }

    @Override
    public boolean deleteById(Long id) {
        return products.deleteByIdReturningCount(id) > 0;
    }

    @Override
    public List<ProductSummaryDto> findSummariesByCategory(Long categoryId) {
        return products.findByCategoryIdOrderByNameAsc(categoryId).stream()
                .map(view -> new ProductSummaryDto(view.getId(), view.getName(), view.getPrice()))
                .toList();
    }

    private CategoryEntity categoryReference(Product product) {
        // the service has already checked that the category exists, so no extra select is needed
        return categories.getReferenceById(product.category().id());
    }
}
