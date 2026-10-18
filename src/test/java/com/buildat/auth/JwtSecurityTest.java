package com.buildat.auth;

import java.time.Instant;
import java.util.*;
import com.buildat.config.SecurityConfiguration;
import com.buildat.http.*;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JwtSecurityTest.ProtectedController.class,
        excludeAutoConfiguration = org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfiguration.class, SecurityProblems.class, Problems.class, RequestIdFilter.class,
        ApiExceptionHandler.class, CurrentIdentity.class, TestAuthConfiguration.class, JwtSecurityTest.ProtectedController.class})
class JwtSecurityTest {
    @RestController static class ProtectedController {
        private final CurrentIdentity identities;
        ProtectedController(CurrentIdentity identities) { this.identities = identities; }
        @GetMapping("/api/v1/me") Map<String, String> me() {
            return Map.of("id", identities.requirePermanentIdentity().subject().toString());
        }
    }
    @Autowired MockMvc mvc;
    @Autowired TestJwtProvider provider;

    @Test void missingAndMalformedAuthenticationUseSafeChallenges() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        for (String token : List.of("broken", "abc.def.ghi")) reject(token);
    }

    @Test void validatesSignatureAndEveryRequiredIdentityClaim() throws Exception {
        var subject = UUID.randomUUID();
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + provider.token(subject)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(subject.toString()));
        reject(TestJwtProvider.token(TestJwtProvider.key("first"), TestJwtProvider.claims(subject), JWSAlgorithm.RS256));
        for (String claim : List.of("sub", "session_id", "exp", "iat", "role", "is_anonymous")) {
            var claims = TestJwtProvider.claims(subject); claims.remove(claim);
            reject(TestJwtProvider.token(provider.first, claims, JWSAlgorithm.RS256));
        }
        Map<String, Object> invalid = Map.of("iss", "https://evil.invalid", "aud", "other", "sub", "bad",
                "session_id", "bad", "role", "service_role", "is_anonymous", "false",
                "exp", Date.from(Instant.now().minusSeconds(120)), "nbf", Date.from(Instant.now().plusSeconds(120)),
                "iat", Date.from(Instant.now().plusSeconds(120)));
        for (var entry : invalid.entrySet()) {
            var claims = TestJwtProvider.claims(subject); claims.put(entry.getKey(), entry.getValue());
            reject(TestJwtProvider.token(provider.first, claims, JWSAlgorithm.RS256));
        }
        reject(TestJwtProvider.token(provider.first, TestJwtProvider.claims(subject), JWSAlgorithm.RS512));
    }

    @Test void anonymousIsAuthenticatedButForbiddenAndOtherRoutesStayDenied() throws Exception {
        var claims = TestJwtProvider.claims(UUID.randomUUID()); claims.put("is_anonymous", true);
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer "
                        + TestJwtProvider.token(provider.first, claims, JWSAlgorithm.RS256)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ANONYMOUS_IDENTITY_NOT_ALLOWED"));
        for (var path : List.of("/api/v1/other", "/login", "/actuator/env", "/v3/api-docs", "/swagger-ui.html")) {
            mvc.perform(get(path).header("Authorization", "Bearer " + provider.token(UUID.randomUUID())))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/me?access_token=" + provider.token(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test void frameworkCachesAndRefreshesTrustedJwksForRotation() throws Exception {
        try (var local = new TestJwtProvider()) {
            var decoder = JwtConfiguration.createDecoder(local.uri(), TestJwtProvider.ISSUER, Set.of("RS256"), "authenticated");
            decoder.decode(local.token(UUID.randomUUID()));
            int fetched = local.requests.get();
            decoder.decode(local.token(UUID.randomUUID()));
            assertThat(local.requests.get()).isEqualTo(fetched);
            local.jwks = new JWKSet(List.of(local.first.toPublicJWK(), local.second.toPublicJWK())).toString();
            decoder.decode(TestJwtProvider.token(local.second, TestJwtProvider.claims(UUID.randomUUID()), JWSAlgorithm.RS256));
            assertThat(local.requests.get()).isGreaterThan(fetched);
            assertThatThrownBy(() -> decoder.decode(TestJwtProvider.token(TestJwtProvider.key("unknown"),
                    TestJwtProvider.claims(UUID.randomUUID()), JWSAlgorithm.RS256))).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
        }
    }

    @Test void providerOutageIsUnavailableAndInvalidConfigurationFailsClosed() throws Exception {
        try (var local = new TestJwtProvider()) {
            local.status = 503;
            JwtDecoder decoder = JwtConfiguration.createDecoder(local.uri(), TestJwtProvider.ISSUER, Set.of("RS256"), "authenticated");
            var exception = catchThrowable(() -> decoder.decode(local.token(UUID.randomUUID())));
            var request = new org.springframework.mock.web.MockHttpServletRequest();
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            new SecurityProblems(new Problems(new tools.jackson.databind.ObjectMapper())).commence(request, response,
                    new org.springframework.security.authentication.AuthenticationServiceException("safe", exception));
            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(response.getContentAsString()).contains("IDENTITY_PROVIDER_UNAVAILABLE");
        }
        assertThatThrownBy(() -> new SupabaseProperties(java.net.URI.create("http://unsafe.invalid"), null, null,
                "authenticated", Set.of("HS256"), "secret")).isInstanceOf(IllegalArgumentException.class);
    }


    @Test void supportsConfiguredEs256RejectsUnsignedAndIgnoresTokenKeyUrls() throws Exception {
        try (var local = new TestJwtProvider(); var untrusted = new TestJwtProvider()) {
            var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder();
            TestJwtProvider.claims(UUID.randomUUID()).forEach(claims::claim);
            var signed = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(local.first.getKeyID()).jwkURL(untrusted.uri()).build(), claims.build());
            signed.sign(new com.nimbusds.jose.crypto.RSASSASigner(local.first));
            var decoder = JwtConfiguration.createDecoder(local.uri(), TestJwtProvider.ISSUER, Set.of("RS256"), "authenticated");
            decoder.decode(signed.serialize());
            assertThat(untrusted.requests.get()).isZero();
            assertThatThrownBy(() -> decoder.decode(new com.nimbusds.jwt.PlainJWT(claims.build()).serialize()))
                    .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
            var hmac = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256), claims.build());
            hmac.sign(new com.nimbusds.jose.crypto.MACSigner(new byte[32]));
            assertThatThrownBy(() -> decoder.decode(hmac.serialize())).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
            var ec = new com.nimbusds.jose.jwk.gen.ECKeyGenerator(com.nimbusds.jose.jwk.Curve.P_256).keyID("ec").generate();
            local.jwks = new JWKSet(ec.toPublicJWK()).toString();
            var elliptic = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader.Builder(JWSAlgorithm.ES256).keyID("ec").build(), claims.build());
            elliptic.sign(new com.nimbusds.jose.crypto.ECDSASigner(ec));
            JwtConfiguration.createDecoder(local.uri(), TestJwtProvider.ISSUER, Set.of("ES256"), "authenticated").decode(elliptic.serialize());
        }
    }

    private void reject(String token) throws Exception {
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("error=\"invalid_token\"")));
    }
}
