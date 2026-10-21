package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.client.ExchangeRateClient;
import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.CategoryDto;
import com.cosmocats.marketplace.dto.ConvertedPriceDto;
import com.cosmocats.marketplace.dto.PageResponse;
import com.cosmocats.marketplace.dto.ProductRequestDto;
import com.cosmocats.marketplace.dto.ProductResponseDto;
import com.cosmocats.marketplace.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.exception.ExternalServiceException;
import com.cosmocats.marketplace.exception.ProductNotFoundException;
import com.cosmocats.marketplace.mapper.ProductMapStructMapper;
import com.cosmocats.marketplace.repository.CategoryRepository;
import com.cosmocats.marketplace.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Category FOOD = new Category(1L, "Space Food");
    private static final Product STAR_TUNA = new Product(1L, "Star Tuna Can", "Tuna caught near Sirius",
            new BigDecimal("9.99"), FOOD, "tuna@cosmocats.space");
    private static final ProductRequestDto REQUEST = new ProductRequestDto("Galaxy Milk", "Anti-gravity milk",
            new BigDecimal("3.50"), 1L, "milk@cosmocats.space");

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ExchangeRateClient exchangeRateClient;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        ProductMapStructMapper mapper = Mappers.getMapper(ProductMapStructMapper.class);
        productService = new ProductService(productRepository, categoryRepository, mapper, exchangeRateClient);
    }

    @Nested
    class CreateProduct {

        @Test
        void savesMappedProductAndReturnsDto() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(FOOD));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.<Product>getArgument(0).withId(6L));

            ProductResponseDto result = productService.createProduct(REQUEST);

            ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(saved.capture());
            assertThat(saved.getValue().id()).as("new product has no id before saving").isNull();
            assertThat(saved.getValue().category()).isEqualTo(FOOD);
            assertThat(result).isEqualTo(new ProductResponseDto(6L, "Galaxy Milk", "Anti-gravity milk",
                    new BigDecimal("3.50"), new CategoryDto(1L, "Space Food"), "milk@cosmocats.space"));
        }

        @Test
        void unknownCategoryIsRejectedAndNothingIsSaved() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(REQUEST))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessage("Category with id 1 does not exist");
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    class GetProductById {

        @Test
        void returnsProduct() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(STAR_TUNA));

            ProductResponseDto result = productService.getProductById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Star Tuna Can");
            assertThat(result.price()).isEqualByComparingTo("9.99");
            assertThat(result.category()).isEqualTo(new CategoryDto(1L, "Space Food"));
        }

        @Test
        void missingProductThrowsNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(99L))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product with id 99 was not found");
        }
    }

    @Nested
    class GetProducts {

        @Test
        void requestsPageOffsetAndWrapsResultInEnvelope() {
            when(productRepository.findAll(4L, 2)).thenReturn(List.of(STAR_TUNA));
            when(productRepository.count()).thenReturn(5L);

            PageResponse<ProductResponseDto> page = productService.getProducts(2, 2);

            assertThat(page.page()).isEqualTo(2);
            assertThat(page.size()).isEqualTo(2);
            assertThat(page.totalElements()).isEqualTo(5L);
            assertThat(page.content()).extracting(ProductResponseDto::id).containsExactly(1L);
        }

        @Test
        void emptyPageHasNoContentButKeepsTotal() {
            when(productRepository.findAll(100L, 10)).thenReturn(List.of());
            when(productRepository.count()).thenReturn(5L);

            PageResponse<ProductResponseDto> page = productService.getProducts(10, 10);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isEqualTo(5L);
        }

        @Test
        void offsetDoesNotOverflowForLargePages() {
            when(productRepository.findAll((long) Integer.MAX_VALUE * 100, 100)).thenReturn(List.of());

            productService.getProducts(Integer.MAX_VALUE, 100);

            verify(productRepository).findAll((long) Integer.MAX_VALUE * 100, 100);
        }
    }

    @Nested
    class UpdateProduct {

        @Test
        void replacesExistingProductKeepingItsId() {
            when(productRepository.existsById(1L)).thenReturn(true);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(FOOD));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            ProductResponseDto result = productService.updateProduct(1L, REQUEST);

            ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(saved.capture());
            assertThat(saved.getValue().id()).isEqualTo(1L);
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Galaxy Milk");
        }

        @Test
        void missingProductThrowsNotFoundAndDoesNotCreateIt() {
            when(productRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> productService.updateProduct(99L, REQUEST))
                    .isInstanceOf(ProductNotFoundException.class);
            verify(productRepository, never()).save(any());
            verifyNoInteractions(categoryRepository);
        }

        @Test
        void unknownCategoryIsRejected() {
            when(productRepository.existsById(1L)).thenReturn(true);
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(1L, REQUEST))
                    .isInstanceOf(CategoryNotFoundException.class);
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteProduct {

        @Test
        void deletesExistingProduct() {
            when(productRepository.existsById(1L)).thenReturn(true);

            productService.deleteProduct(1L);

            verify(productRepository).deleteById(1L);
        }

        @Test
        void missingProductThrowsNotFound() {
            when(productRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> productService.deleteProduct(99L))
                    .isInstanceOf(ProductNotFoundException.class);
            verify(productRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    class ConvertPrice {

        @Test
        void multipliesPriceByRateAndRoundsHalfEven() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(STAR_TUNA));
            when(exchangeRateClient.getRate("EUR")).thenReturn(new BigDecimal("1.5"));

            ConvertedPriceDto result = productService.convertPrice(1L, "EUR");

            // 9.99 * 1.5 = 14.985 -> HALF_EVEN -> 14.98
            assertThat(result).isEqualTo(new ConvertedPriceDto(1L, "EUR", new BigDecimal("1.5"), new BigDecimal("14.98")));
        }

        @Test
        void missingProductIsReportedBeforeCallingExchangeService() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.convertPrice(99L, "EUR"))
                    .isInstanceOf(ProductNotFoundException.class);
            verifyNoInteractions(exchangeRateClient);
        }

        @Test
        void exchangeServiceFailureIsPropagated() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(STAR_TUNA));
            when(exchangeRateClient.getRate("EUR")).thenThrow(new ExternalServiceException("down"));

            assertThatThrownBy(() -> productService.convertPrice(1L, "EUR"))
                    .isInstanceOf(ExternalServiceException.class);
        }
    }
}
