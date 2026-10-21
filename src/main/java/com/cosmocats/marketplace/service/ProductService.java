package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.client.ExchangeRateClient;
import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.dto.ConvertedPriceDto;
import com.cosmocats.marketplace.dto.PageResponse;
import com.cosmocats.marketplace.dto.ProductRequestDto;
import com.cosmocats.marketplace.dto.ProductResponseDto;
import com.cosmocats.marketplace.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.exception.ProductNotFoundException;
import com.cosmocats.marketplace.mapper.ProductMapStructMapper;
import com.cosmocats.marketplace.repository.CategoryRepository;
import com.cosmocats.marketplace.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapStructMapper productMapper;
    private final ExchangeRateClient exchangeRateClient;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          ProductMapStructMapper productMapper,
                          ExchangeRateClient exchangeRateClient) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
        this.exchangeRateClient = exchangeRateClient;
    }

    public ProductResponseDto createProduct(ProductRequestDto request) {
        Product product = productMapper.toDomain(request, findCategory(request.categoryId()));
        Product saved = productRepository.save(product);
        log.info("Created product id={} name='{}'", saved.id(), saved.name());
        return productMapper.toDto(saved);
    }

    public ProductResponseDto getProductById(Long id) {
        return productMapper.toDto(findProduct(id));
    }

    public PageResponse<ProductResponseDto> getProducts(int page, int size) {
        List<ProductResponseDto> content = productRepository.findAll((long) page * size, size).stream()
                .map(productMapper::toDto)
                .toList();
        return new PageResponse<>(content, page, size, productRepository.count());
    }

    public ProductResponseDto updateProduct(Long id, ProductRequestDto request) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        Product updated = productMapper.toDomain(request, findCategory(request.categoryId())).withId(id);
        Product saved = productRepository.update(updated).orElseThrow(() -> new ProductNotFoundException(id));
        log.info("Updated product id={}", id);
        return productMapper.toDto(saved);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.deleteById(id)) {
            throw new ProductNotFoundException(id);
        }
        log.info("Deleted product id={}", id);
    }

    public ConvertedPriceDto convertPrice(Long id, String currency) {
        Product product = findProduct(id);
        BigDecimal rate = exchangeRateClient.getRate(currency);
        BigDecimal amount = product.price().multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
        return new ConvertedPriceDto(product.id(), currency, rate, amount);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
