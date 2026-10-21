package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.exception.ExternalServiceException;
import com.cosmocats.marketplace.exception.ProductNotFoundException;
import com.cosmocats.marketplace.exception.UnsupportedCurrencyException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Translates exceptions into RFC 9457 "Problem Details for HTTP APIs" responses
 * ({@code application/problem+json}) with type, title, status, detail and instance.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROBLEM_BASE_URI = "https://cosmo-cats.market/errors/";
    private static final URI ABOUT_BLANK = URI.create("about:blank");

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return validationProblem(violations, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldViolation> violations = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldViolation(fieldName(result.getMethodParameter().getParameterName(), error),
                                error.getDefaultMessage())))
                .toList();
        return validationProblem(violations, request);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "product-not-found", "Product not found", ex.getMessage(), request);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "category-not-found", "Category not found",
                ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedCurrencyException.class)
    public ProblemDetail handleUnsupportedCurrency(UnsupportedCurrencyException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "unsupported-currency", "Unsupported currency",
                ex.getMessage(), request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ProblemDetail handleExternalService(ExternalServiceException ex, HttpServletRequest request) {
        log.warn("External service failure: {}", ex.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "external-service", "External service error",
                "A dependent service failed to respond correctly. Please try again later.", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal", "Internal server error",
                "An unexpected error occurred. Please contact support if the problem persists.", request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        // Syntactically valid JSON with a value of the wrong type (e.g. "price": "abc"):
        // report it as a validation error of that field, not as broken JSON.
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            return validationProblem(List.of(new FieldViolation(jsonPath(mismatch), typeMessage(mismatch))), request);
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body is missing or is not valid JSON.");
        problem.setType(URI.create(PROBLEM_BASE_URI + "malformed-request"));
        problem.setTitle("Malformed request");
        problem.setInstance(URI.create(requestUri(request)));
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Completes the problems produced by the base class (405, 415, type mismatch, ...):
     * every response gets a concrete {@code type} and an {@code instance}.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            if (problem.getType() == null || ABOUT_BLANK.equals(problem.getType())) {
                HttpStatus status = HttpStatus.resolve(statusCode.value());
                String slug = status != null ? status.name().toLowerCase(Locale.ROOT).replace('_', '-') : "http-error";
                problem.setType(URI.create(PROBLEM_BASE_URI + slug));
            }
            if (problem.getInstance() == null) {
                problem.setInstance(URI.create(requestUri(request)));
            }
        }
        return response;
    }

    private ResponseEntity<Object> validationProblem(List<FieldViolation> unsorted, WebRequest request) {
        List<FieldViolation> violations = unsorted.stream()
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList();
        String detail = violations.stream()
                .map(violation -> "Field '" + violation.field() + "' " + violation.message() + ".")
                .collect(Collectors.joining(" "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation"));
        problem.setTitle("Validation failed");
        problem.setInstance(URI.create(requestUri(request)));
        problem.setProperty("errors", violations);
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail,
                                         HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE_URI + type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private static String requestUri(WebRequest request) {
        // Spring MVC always passes a ServletWebRequest to ResponseEntityExceptionHandler
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }

    private static String jsonPath(MismatchedInputException ex) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : ex.getPath()) {
            if (reference.getPropertyName() != null) {
                path.append(path.isEmpty() ? "" : ".").append(reference.getPropertyName());
            } else {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String typeMessage(MismatchedInputException ex) {
        Class<?> target = ex.getTargetType();
        if (target != null && (Number.class.isAssignableFrom(target) || (target.isPrimitive() && target != boolean.class))) {
            return "must be a number";
        }
        if (target == String.class) {
            return "must be a string";
        }
        return "has an invalid value";
    }

    private static String fieldName(String parameterName, MessageSourceResolvable error) {
        return error instanceof FieldError fieldError ? fieldError.getField() : parameterName;
    }

    /** One invalid field of the request, exposed as an RFC 9457 extension member {@code errors}. */
    public record FieldViolation(String field, String message) {
    }
}
