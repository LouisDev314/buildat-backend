package com.buildat.auth;

import java.util.UUID;
import com.buildat.http.ApiFailure;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentIdentity {
    public record Identity(String issuer, UUID subject, String accessToken) {
        @Override public String toString() { return "Identity[redacted]"; }
    }

    public Identity requirePermanentIdentity() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            throw new ApiFailure(401, "AUTHENTICATION_REQUIRED");
        }
        var jwt = token.getToken();
        if (!Boolean.FALSE.equals(jwt.getClaims().get("is_anonymous"))) {
            throw new ApiFailure(403, "ANONYMOUS_IDENTITY_NOT_ALLOWED");
        }
        return new Identity(jwt.getIssuer().toString(), UUID.fromString(jwt.getSubject()), jwt.getTokenValue());
    }
}
