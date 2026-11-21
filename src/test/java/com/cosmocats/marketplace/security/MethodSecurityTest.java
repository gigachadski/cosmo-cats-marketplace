package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.client.ExchangeRateClient;
import com.cosmocats.marketplace.dto.CategoryRequestDto;
import com.cosmocats.marketplace.dto.ProductRequestDto;
import com.cosmocats.marketplace.mapper.ProductMapStructMapper;
import com.cosmocats.marketplace.repository.CategoryRepository;
import com.cosmocats.marketplace.repository.OrderRepository;
import com.cosmocats.marketplace.repository.ProductRepository;
import com.cosmocats.marketplace.service.CategoryService;
import com.cosmocats.marketplace.service.OrderAction;
import com.cosmocats.marketplace.service.OrderService;
import com.cosmocats.marketplace.service.ProductService;
import com.cosmocats.marketplace.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@code @PreAuthorize} rules on the services, with real services and mocked repositories. */
@SpringBootTest(classes = {MethodSecurityConfig.class, ProductService.class, CategoryService.class,
        OrderService.class, ReportService.class})
class MethodSecurityTest {

    private static final UUID ORDER = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final ProductRequestDto PRODUCT = new ProductRequestDto("Star Tuna", null, BigDecimal.ONE, 1L, "a@b.space");

    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ReportService reportService;

    @MockitoBean
    private ProductRepository productRepository;
    @MockitoBean
    private CategoryRepository categoryRepository;
    @MockitoBean
    private OrderRepository orderRepository;
    @MockitoBean
    private ProductMapStructMapper productMapper;
    @MockitoBean
    private ExchangeRateClient exchangeRateClient;
    @MockitoBean
    private Clock clock;

    @Test
    void anonymousCallerIsRejected() {
        assertThatThrownBy(() -> productService.getProducts(0, 10))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCanReadButNotChangeTheCatalogue() {
        assertThatCode(() -> productService.getProducts(0, 10)).doesNotThrowAnyException();
        assertThatCode(categoryService::getCategories).doesNotThrowAnyException();

        assertThatThrownBy(() -> productService.createProduct(PRODUCT)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> productService.deleteProduct(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> categoryService.createCategory(new CategoryRequestDto("Star")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanChangeTheCatalogue() {
        // passes the security check and reaches the (mocked) repository, which reports "not found"
        assertThatThrownBy(() -> productService.deleteProduct(1L))
                .isNotInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isNotInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderAction.class, names = {"PAY", "CANCEL"})
    @WithMockUser(roles = "USER")
    void customerMayPayOrCancel(OrderAction action) {
        assertThatThrownBy(() -> orderService.changeStatus(ORDER, action))
                .isNotInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderAction.class, names = {"SHIP", "DELIVER"})
    @WithMockUser(roles = "USER")
    void customerMayNotShipOrDeliver(OrderAction action) {
        assertThatThrownBy(() -> orderService.changeStatus(ORDER, action)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "USER")
    void onlyAdminDeletesOrders() {
        assertThatThrownBy(() -> orderService.deleteOrder(ORDER)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "USER")
    void customerCannotReadCommitteeReports() {
        assertThatThrownBy(() -> reportService.getTopSellingProducts(5)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "committee", roles = "REPORTS")
    void apiKeyClientReadsReports() {
        assertThatCode(() -> reportService.getTopSellingProducts(5)).doesNotThrowAnyException();
    }
}
