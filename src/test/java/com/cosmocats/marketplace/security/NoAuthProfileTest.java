package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.dto.CategoryDto;
import com.cosmocats.marketplace.dto.ProductResponseDto;
import com.cosmocats.marketplace.service.ProductService;
import com.cosmocats.marketplace.web.ProductController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With the "no-auth" profile every request is allowed without a token or session. */
@WebMvcTest(ProductController.class)
@ActiveProfiles(NoAuthSecurityConfig.PROFILE)
@Import(NoAuthSecurityConfig.class)
class NoAuthProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void anonymousRequestsAreAllowed() throws Exception {
        ProductResponseDto product = new ProductResponseDto(1L, "Star Tuna", null, BigDecimal.ONE,
                new CategoryDto(1L, "Space Food"), "a@b.space");
        when(productService.getProductById(1L)).thenReturn(product);
        when(productService.createProduct(any())).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/1")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Star Tuna", "price": 1, "categoryId": 1, "sellerEmail": "a@b.space"}
                        """))
                .andExpect(status().isCreated());
    }
}
