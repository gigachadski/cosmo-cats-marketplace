package com.cosmocats.marketplace.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

/**
 * Authenticates machine clients by the {@code X-API-Key} header. A missing or unknown key
 * stops the request with 401; a valid key authenticates the client with role {@code REPORTS}.
 * <p>
 * Not a Spring bean on purpose: as a bean it would also be registered as a servlet filter for every URL.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    static final String ROLE = "ROLE_REPORTS";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final Map<String, String> keysByClient;
    private final AuthenticationEntryPoint entryPoint;

    public ApiKeyAuthenticationFilter(ApiKeyProperties properties, AuthenticationEntryPoint entryPoint) {
        this.keysByClient = properties.apiKeys();
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (presented == null || presented.isBlank()) {
            entryPoint.commence(request, response, new BadCredentialsException("API key is missing in the " + HEADER + " header"));
            return;
        }
        Optional<String> client = findClient(presented.strip());
        if (client.isEmpty()) {
            log.warn("Rejected request to {} with an invalid API key", request.getRequestURI());
            entryPoint.commence(request, response, new BadCredentialsException("API key is invalid"));
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                client.get(), null, AuthorityUtils.createAuthorityList(ROLE)));
        SecurityContextHolder.setContext(context);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<String> findClient(String presented) {
        byte[] presentedBytes = presented.getBytes(StandardCharsets.UTF_8);
        // constant-time comparison: the response time does not reveal how much of a key matched
        return keysByClient.entrySet().stream()
                .filter(entry -> MessageDigest.isEqual(entry.getValue().getBytes(StandardCharsets.UTF_8), presentedBytes))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
