package com.buildat.auth;

import java.time.Clock;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class IdentityClaimsValidator implements OAuth2TokenValidator<Jwt> {
    private final Clock clock;
    public IdentityClaimsValidator(Clock clock) { this.clock = clock; }

    @Override public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            if (!canonicalUuid(jwt.getClaims().get("sub"))
                    || !canonicalUuid(jwt.getClaims().get("session_id"))
                    || !"authenticated".equals(jwt.getClaims().get("role"))
                    || !(jwt.getClaims().get("is_anonymous") instanceof Boolean)
                    || jwt.getExpiresAt() == null || jwt.getIssuedAt() == null
                    || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                    || jwt.getIssuedAt().isAfter(clock.instant().plusSeconds(60))) {
                return invalid();
            }
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException | ClassCastException exception) {
            return invalid();
        }
    }

    private static boolean canonicalUuid(Object value) {
        return value instanceof String text && UUID.fromString(text).toString().equals(text);
    }
    private static OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid identity claims", null));
    }
}
