package com.buildat.auth;

import java.io.IOException;
import com.buildat.http.Problems;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblems implements AuthenticationEntryPoint {
    private final Problems problems;
    public SecurityProblems(Problems problems) { this.problems = problems; }
    @Override public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        if (providerUnavailable(exception)) {
            problems.write(request, response, 503, "IDENTITY_PROVIDER_UNAVAILABLE");
            return;
        }
        // Preserve protocol semantics without copying exception descriptions into headers or JSON.
        new BearerTokenAuthenticationEntryPoint().commence(request, response,
                new org.springframework.security.authentication.InsufficientAuthenticationException("Authentication required"));
        boolean supplied = request.getHeader(HttpHeaders.AUTHORIZATION) != null
                && request.getHeader(HttpHeaders.AUTHORIZATION).regionMatches(true, 0, "Bearer", 0, 6);
        if (supplied) response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                response.getHeader(HttpHeaders.WWW_AUTHENTICATE) + ", error=\"invalid_token\"");
        problems.write(request, response, 401, supplied ? "INVALID_TOKEN" : "AUTHENTICATION_REQUIRED");
    }
    private static boolean providerUnavailable(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.springframework.web.client.ResourceAccessException
                    || cause instanceof org.springframework.web.client.RestClientResponseException
                    || cause instanceof com.nimbusds.jose.RemoteKeySourceException) return true;
        }
        return false;
    }
}
