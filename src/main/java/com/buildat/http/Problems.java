package com.buildat.http;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class Problems {
    private final ObjectMapper mapper;

    public Problems(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static ProblemDetail create(int status, String code, String detail, HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatusCode.valueOf(status), detail);
        var knownStatus = HttpStatus.resolve(status);
        problem.setTitle(knownStatus == null ? "Request failed" : knownStatus.getReasonPhrase());
        problem.setType(URI.create("urn:buildat:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        // getRequestURI excludes query parameters. Fall back for malformed request targets.
        try {
            problem.setInstance(URI.create(request.getRequestURI()));
        } catch (IllegalArgumentException ignored) {
            problem.setInstance(URI.create("/"));
        }
        problem.setProperty("code", code);
        problem.setProperty("requestId", RequestIdFilter.requestId(request));
        return problem;
    }

    public static String code(int status) {
        return switch (status) {
            case 400 -> "INVALID_REQUEST";
            case 401 -> "AUTHENTICATION_REQUIRED";
            case 403 -> "ACCESS_DENIED";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> status >= 500 ? "INTERNAL_ERROR" : "REQUEST_FAILED";
        };
    }

    public static String detail(int status) {
        return switch (status) {
            case 400 -> "The request is invalid.";
            case 401 -> "Authentication is required.";
            case 403 -> "This action is not permitted.";
            case 404 -> "The requested resource was not found.";
            case 405 -> "The request method is not supported.";
            case 413 -> "The request body is too large.";
            case 415 -> "The request media type is not supported.";
            case 503 -> "A required service is unavailable.";
            default -> status >= 500 ? "An unexpected error occurred." : "The request could not be completed.";
        };
    }

    public void write(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), create(status, code(status), detail(status), request));
    }
}
