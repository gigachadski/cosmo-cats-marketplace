package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.dto.CategoryDto;
import com.cosmocats.marketplace.dto.CategoryRequestDto;
import com.cosmocats.marketplace.dto.ProductSummaryDto;
import com.cosmocats.marketplace.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.repository.CategoryRepository;
import com.cosmocats.marketplace.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Category FOOD = new Category(1L, "Space Food");

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void listsAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(FOOD, new Category(2L, "Zero-G Toys")));

        assertThat(categoryService.getCategories())
                .containsExactly(new CategoryDto(1L, "Space Food"), new CategoryDto(2L, "Zero-G Toys"));
    }

    @Test
    void returnsCategoryById() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(FOOD));

        assertThat(categoryService.getCategory(1L)).isEqualTo(new CategoryDto(1L, "Space Food"));
    }

    @Test
    void missingCategoryIsNotFound() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(9L))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category with id 9 was not found");
    }

    @Test
    void returnsProductsOfExistingCategory() {
        List<ProductSummaryDto> summaries = List.of(new ProductSummaryDto(4L, "Star Tuna", BigDecimal.ONE));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(FOOD));
        when(productRepository.findSummariesByCategory(1L)).thenReturn(summaries);

        assertThat(categoryService.getCategoryProducts(1L)).isEqualTo(summaries);
    }

    @Test
    void productsOfMissingCategoryAreNotFound() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryProducts(9L)).isInstanceOf(CategoryNotFoundException.class);
        verifyNoInteractions(productRepository);
    }

    @Test
    void createSavesTrimmedNameWithoutId() {
        when(categoryRepository.save(new Category(null, "Space Food"))).thenReturn(FOOD);

        assertThat(categoryService.createCategory(new CategoryRequestDto("  Space Food "))).isEqualTo(new CategoryDto(1L, "Space Food"));
    }

    @Test
    void updateRenamesExistingCategory() {
        when(categoryRepository.update(new Category(1L, "Cosmic Food"))).thenReturn(Optional.of(new Category(1L, "Cosmic Food")));

        assertThat(categoryService.updateCategory(1L, new CategoryRequestDto("Cosmic Food")))
                .isEqualTo(new CategoryDto(1L, "Cosmic Food"));
    }

    @Test
    void updateOfMissingCategoryIsNotFound() {
        when(categoryRepository.update(new Category(9L, "X Star"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(9L, new CategoryRequestDto("X Star")))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void deleteRemovesCategory() {
        when(categoryRepository.deleteById(1L)).thenReturn(true);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteOfMissingCategoryIsNotFound() {
        when(categoryRepository.deleteById(9L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteCategory(9L)).isInstanceOf(CategoryNotFoundException.class);
    }
}
