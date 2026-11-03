package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.dto.CategoryDto;
import com.cosmocats.marketplace.dto.CategoryRequestDto;
import com.cosmocats.marketplace.dto.ProductSummaryDto;
import com.cosmocats.marketplace.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.service.CategoryService;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    private static final String CATEGORIES = "/api/v1/categories";
    private static final String ERRORS = "https://cosmo-cats.market/errors/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    private static DataIntegrityViolationException violationOf(String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("violates " + constraint, new SQLException(), constraint));
    }

    @Test
    void listsCategories() throws Exception {
        when(categoryService.getCategories()).thenReturn(List.of(new CategoryDto(1L, "Space Food")));

        mockMvc.perform(get(CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Space Food"));
    }

    @Test
    void returnsCategoryAndItsProducts() throws Exception {
        when(categoryService.getCategory(1L)).thenReturn(new CategoryDto(1L, "Space Food"));
        when(categoryService.getCategoryProducts(1L))
                .thenReturn(List.of(new ProductSummaryDto(4L, "Star Tuna", new BigDecimal("9.99"))));

        mockMvc.perform(get(CATEGORIES + "/1")).andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get(CATEGORIES + "/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price").value(9.99));
    }

    @Test
    void missingCategoryIsNotFound() throws Exception {
        when(categoryService.getCategory(9L)).thenThrow(new CategoryNotFoundException(9L));

        mockMvc.perform(get(CATEGORIES + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ERRORS + "category-not-found"))
                .andExpect(jsonPath("$.instance").value(CATEGORIES + "/9"));
    }

    @Test
    void validCategoryIsCreated() throws Exception {
        when(categoryService.createCategory(new CategoryRequestDto("Space Food"))).thenReturn(new CategoryDto(3L, "Space Food"));

        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Space Food\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(CATEGORIES + "/3")));
    }

    @ParameterizedTest(name = "name={0}")
    @ValueSource(strings = {"{}", "{\"name\": \"\"}", "{\"name\": \"   \"}"})
    void blankNameIsRejected(String json) throws Exception {
        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERRORS + "validation"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        verifyNoInteractions(categoryService);
    }

    @Test
    void tooLongNameIsRejected() throws Exception {
        String json = "{\"name\": \"" + "x".repeat(101) + "\"}";

        mockMvc.perform(put(CATEGORIES + "/1").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("must be at most 100 characters"));
    }

    @Test
    void renameReturnsUpdatedCategory() throws Exception {
        when(categoryService.updateCategory(1L, new CategoryRequestDto("Cosmic Food"))).thenReturn(new CategoryDto(1L, "Cosmic Food"));

        mockMvc.perform(put(CATEGORIES + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Cosmic Food\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cosmic Food"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(CATEGORIES + "/1")).andExpect(status().isNoContent());
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "uq_category_name, category-already-exists",
            "UQ_PRODUCT_CATEGORY_NAME, product-already-exists",
            "fk_product_category, category-in-use",
            "fk_order_item_product, product-in-use",
            "some_other_constraint, conflict"})
    void databaseConstraintViolationsBecomeConflicts(String constraint, String type) throws Exception {
        doThrow(violationOf(constraint)).when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete(CATEGORIES + "/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ERRORS + type))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void violationWithoutConstraintNameIsGenericConflict() throws Exception {
        when(categoryService.createCategory(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        mockMvc.perform(post(CATEGORIES).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Space Food\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ERRORS + "conflict"));
    }
}
