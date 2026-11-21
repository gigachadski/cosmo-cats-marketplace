package com.cosmocats.marketplace;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real OAuth 2.0 Bearer tokens end to end: tokens are signed with an RSA key whose public part
 * is published as JWKS by WireMock, exactly like the authorization server in docker-compose.
 */
class JwtAuthenticationIT extends AbstractIt {

    private static final String ISSUER = "https://auth.cosmo-cats.market";
    private static final RSAKey SIGNING_KEY = generateKey("cosmo-cats-test-key");
    private static final RSAKey FOREIGN_KEY = generateKey("cosmo-cats-test-key");

    private static RSAKey generateKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String token(RSAKey key, String issuer, Instant expiresAt, String... roles) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("captain-whiskers")
                .claim("roles", List.of(roles))
                .issueTime(Date.from(Instant.now().minusSeconds(60)))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(key.getKeyID()).type(JOSEObjectType.JWT).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return "Bearer " + jwt.serialize();
    }

    private static String validToken(String... roles) throws Exception {
        return token(SIGNING_KEY, ISSUER, Instant.now().plusSeconds(600), roles);
    }

    @BeforeEach
    void publishJwks() {
        exchangeService.stubFor(WireMock.get(urlEqualTo("/.well-known/jwks.json"))
                .willReturn(okJson(new JWKSet(SIGNING_KEY.toPublicJWK()).toString())));
    }

    @Test
    void userTokenReadsTheCatalogue() throws Exception {
        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, validToken("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void userTokenCannotChangeTheCatalogue() throws Exception {
        mockMvc.perform(post("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, validToken("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Space Food\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/forbidden"));
    }

    @Test
    void adminTokenChangesTheCatalogue() throws Exception {
        mockMvc.perform(post("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, validToken("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Space Food\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void tokenWithoutRolesIsAuthenticatedButForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, validToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/unauthorized"));
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        String expired = token(SIGNING_KEY, ISSUER, Instant.now().minusSeconds(3600), "ADMIN");

        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFromAnotherIssuerIsUnauthorized() throws Exception {
        String foreignIssuer = token(SIGNING_KEY, "https://evil.example", Instant.now().plusSeconds(600), "ADMIN");

        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, foreignIssuer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyIsUnauthorized() throws Exception {
        String forged = token(FOREIGN_KEY, ISSUER, Instant.now().plusSeconds(600), "ADMIN");

        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
