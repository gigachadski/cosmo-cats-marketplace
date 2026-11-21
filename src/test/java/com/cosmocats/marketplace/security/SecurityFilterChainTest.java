package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.dto.PageResponse;
import com.cosmocats.marketplace.service.ProductService;
import com.cosmocats.marketplace.service.ReportService;
import com.cosmocats.marketplace.web.ProductController;
import com.cosmocats.marketplace.web.ReportController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The real filter chains of {@link SecurityConfig}: Bearer JWT for the API, API key for reports. */
@WebMvcTest({ProductController.class, ReportController.class})
@Import({SecurityConfig.class, ProblemDetailSecurityHandler.class})
class SecurityFilterChainTest {

    private static final String PRODUCTS = "/api/v1/products";
    private static final String REPORTS = "/api/v1/reports/top-products";
    private static final String API_KEY = "dev-committee-key-change-me";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static Jwt jwtWithRoles(String... roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("captain-whiskers")
                .claim("roles", List.of(roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Nested
    class BearerToken {

        @Test
        void requestWithoutTokenIsUnauthorized() throws Exception {
            mockMvc.perform(get(PRODUCTS))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/unauthorized"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.instance").value(PRODUCTS));
            verifyNoInteractions(productService);
        }

        @Test
        void validTokenWithRolesClaimIsAccepted() throws Exception {
            when(jwtDecoder.decode("good-token")).thenReturn(jwtWithRoles("USER"));
            when(productService.getProducts(0, 10)).thenReturn(new PageResponse<>(List.of(), 0, 10, 0));

            mockMvc.perform(get(PRODUCTS).header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void invalidTokenIsUnauthorized() throws Exception {
            when(jwtDecoder.decode("forged")).thenThrow(new BadJwtException("Signed JWT rejected: Invalid signature"));

            mockMvc.perform(get(PRODUCTS).header(HttpHeaders.AUTHORIZATION, "Bearer forged"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/unauthorized"));
            verifyNoInteractions(productService);
        }

        @Test
        void mockJwtPostProcessorAuthenticatesRequest() throws Exception {
            when(productService.getProducts(0, 10)).thenReturn(new PageResponse<>(List.of(), 0, 10, 0));

            mockMvc.perform(get(PRODUCTS).with(jwt().jwt(jwtWithRoles("ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        void rolesClaimBecomesSpringRoles() {
            var authentication = SecurityConfig.rolesFromJwt().convert(jwtWithRoles("USER", "ADMIN"));

            assertThat(authentication.getAuthorities())
                    .extracting(Object::toString)
                    .filteredOn(authority -> authority.startsWith("ROLE_"))
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }
    }

    @Nested
    class ApiKey {

        @Test
        void missingKeyIsUnauthorized() throws Exception {
            mockMvc.perform(get(REPORTS))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "ApiKey"))
                    .andExpect(jsonPath("$.detail").value("API key is missing in the X-API-Key header"));
            verifyNoInteractions(reportService);
        }

        @Test
        void wrongKeyIsUnauthorized() throws Exception {
            mockMvc.perform(get(REPORTS).header(ApiKeyAuthenticationFilter.HEADER, "stolen-key"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("API key is invalid"));
            verifyNoInteractions(reportService);
        }

        @Test
        void validKeyIsAccepted() throws Exception {
            when(reportService.getTopSellingProducts(10)).thenReturn(List.of());

            mockMvc.perform(get(REPORTS).header(ApiKeyAuthenticationFilter.HEADER, " " + API_KEY + " "))
                    .andExpect(status().isOk());
        }

        @Test
        void bearerTokenDoesNotOpenReports() throws Exception {
            mockMvc.perform(get(REPORTS).with(jwt().jwt(jwtWithRoles("ADMIN"))))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(reportService);
        }
    }
}
