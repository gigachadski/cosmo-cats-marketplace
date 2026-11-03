package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.dto.CategoryDto;
import com.cosmocats.marketplace.dto.CategoryRequestDto;
import com.cosmocats.marketplace.dto.ProductSummaryDto;
import com.cosmocats.marketplace.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.repository.CategoryRepository;
import com.cosmocats.marketplace.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll().stream().map(CategoryService::toDto).toList();
    }

    public CategoryDto getCategory(Long id) {
        return toDto(findCategory(id));
    }

    public List<ProductSummaryDto> getCategoryProducts(Long id) {
        findCategory(id);
        return productRepository.findSummariesByCategory(id);
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequestDto request) {
        Category saved = categoryRepository.save(new Category(null, request.name().strip()));
        log.info("Created category id={} name='{}'", saved.id(), saved.name());
        return toDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequestDto request) {
        Category updated = categoryRepository.update(new Category(id, request.name().strip()))
                .orElseThrow(() -> new CategoryNotFoundException(id));
        log.info("Renamed category id={} to '{}'", id, updated.name());
        return toDto(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.deleteById(id)) {
            throw new CategoryNotFoundException(id);
        }
        log.info("Deleted category id={}", id);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private static CategoryDto toDto(Category category) {
        return new CategoryDto(category.id(), category.name());
    }
}
