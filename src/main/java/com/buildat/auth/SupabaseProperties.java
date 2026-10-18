package com.buildat.auth;

import java.net.URI;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("buildat.auth")
public record SupabaseProperties(URI projectUrl, URI issuer, URI jwksUri, String audience,
        Set<String> algorithms, String publishableKey) {
    public SupabaseProperties {
        requireHttps(projectUrl);
        requireHttps(issuer);
        requireHttps(jwksUri);
        if ((projectUrl.getPath() != null && !projectUrl.getPath().isEmpty() && !projectUrl.getPath().equals("/"))
                || !issuer.equals(projectUrl.resolve("/auth/v1"))
                || !jwksUri.equals(projectUrl.resolve("/auth/v1/.well-known/jwks.json"))
                || audience == null || audience.isBlank()
                || algorithms == null || algorithms.isEmpty()
                || !Set.of("ES256", "RS256").containsAll(algorithms)
                || publishableKey == null || !publishableKey.startsWith("sb_publishable_")) {
            throw new IllegalArgumentException("Invalid Supabase authentication configuration");
        }
    }

    private static void requireHttps(URI uri) {
        if (uri == null || !"https".equals(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Supabase authentication requires trusted HTTPS endpoints");
        }
    }

    // Configuration binding errors and diagnostics must not print keys.
    @Override public String toString() { return "SupabaseProperties[redacted]"; }
}
