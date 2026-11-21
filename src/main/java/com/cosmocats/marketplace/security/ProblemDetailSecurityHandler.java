package com.cosmocats.marketplace.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;

/**
 * Answers security failures in the same RFC 9457 format as the rest of the API:
 * 401 when the caller is not authenticated, 403 when it is but lacks the role.
 */
@Component
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String PROBLEM_BASE_URI = "https://cosmo-cats.market/errors/";

    private final JsonMapper jsonMapper;

    public ProblemDetailSecurityHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        String challenge = request.getRequestURI().startsWith(SecurityConfig.REPORTS_PATH_PREFIX) ? "ApiKey" : "Bearer";
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);
        write(response, HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication required",
                exception.getMessage(), request);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "forbidden", "Access denied",
                "You do not have permission to perform this action.", request);
    }

    private void write(HttpServletResponse response, HttpStatus status, String type, String title,
                       String detail, HttpServletRequest request) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE_URI + type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
