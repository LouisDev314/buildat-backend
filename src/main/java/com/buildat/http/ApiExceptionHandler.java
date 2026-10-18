package com.buildat.http;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    public record FieldError(String field, String code, String message) { }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var servletRequest = ((ServletWebRequest) request).getRequest();
        var code = Problems.code(status.value());
        var detail = Problems.detail(status.value());
        if (ex instanceof MethodArgumentNotValidException) {
            code = "VALIDATION_FAILED";
            detail = "One or more fields are invalid.";
        }
        var problem = Problems.create(status.value(), code, detail, servletRequest);
        if (ex instanceof MethodArgumentNotValidException invalid) {
            List<FieldError> errors = invalid.getBindingResult().getFieldErrors().stream()
                    .map(error -> new FieldError(error.getField(), "INVALID_VALUE", "The value is invalid."))
                    .toList();
            problem.setProperty("errors", errors);
        }
        // Keep framework protocol headers (for example Allow and Accept).
        return new ResponseEntity<>(problem, headers, status);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex, HttpServletRequest request) {
        // Do not log exception text: it may contain SQL, credentials or submitted content.
        logger.error("Unexpected request failure; requestId=" + RequestIdFilter.requestId(request));
        return ResponseEntity.internalServerError()
                .body(Problems.create(500, "INTERNAL_ERROR", Problems.detail(500), request));
    }
}
