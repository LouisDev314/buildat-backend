package com.buildat.auth;

import java.util.UUID;
import com.buildat.http.ApiFailure;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SupabaseUserVerifierTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://test.invalid")
            .defaultHeader("apikey", "sb_publishable_test");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final SupabaseUserVerifier verifier = new SupabaseUserVerifier(builder.build());
    private final CurrentIdentity.Identity identity = new CurrentIdentity.Identity(TestJwtProvider.ISSUER, UUID.randomUUID(), "synthetic-token");

    @Test void verifiesMatchingPermanentConfirmedUserWithoutAdminCredentials() {
        server.expect(requestTo("https://test.invalid/auth/v1/user"))
                .andExpect(header("Authorization", "Bearer synthetic-token"))
                .andExpect(header("apikey", "sb_publishable_test"))
                .andRespond(withSuccess(user("false", "\"2026-10-18T00:00:00Z\""), MediaType.APPLICATION_JSON));
        verifier.verify(identity); server.verify();
    }
    @Test void unconfirmedAndAnonymousAreForbidden() {
        assertResponse(user("false", "null"), 403, "EMAIL_CONFIRMATION_REQUIRED");
        assertResponse(user("true", "null"), 403, "ANONYMOUS_IDENTITY_NOT_ALLOWED");
    }
    @Test void missingMismatchedAndMalformedProviderIdentityFailClosed() {
        for (var body : java.util.List.of("{}", "{broken", user("false", "\"bad-date\""),
                user("false", "null").replace(identity.subject().toString(), UUID.randomUUID().toString()))) {
            assertResponse(body, 503, "IDENTITY_PROVIDER_UNAVAILABLE");
        }
    }
    @Test void definitiveRejectionDiffersFromTemporaryProviderFailure() {
        for (int status : new int[]{401, 404, 429, 500, 503}) {
            server.reset(); server.expect(anything()).andRespond(withStatus(HttpStatusCode.valueOf(status)));
            assertThatThrownBy(() -> verifier.verify(identity)).isInstanceOfSatisfying(ApiFailure.class,
                    failure -> assertThat(failure.status()).isEqualTo(status == 401 || status == 404 ? 401 : 503));
        }
        server.reset(); server.expect(anything()).andRespond(withException(new java.net.SocketTimeoutException("private")));
        assertThatThrownBy(() -> verifier.verify(identity)).isInstanceOfSatisfying(ApiFailure.class,
                failure -> assertThat(failure.code()).isEqualTo("IDENTITY_PROVIDER_UNAVAILABLE"));
    }
    private String user(String anonymous, String confirmation) {
        return "{\"id\":\"" + identity.subject() + "\",\"is_anonymous\":" + anonymous
                + ",\"email\":\"synthetic@example.invalid\",\"email_confirmed_at\":" + confirmation + "}";
    }
    private void assertResponse(String body, int status, String code) {
        server.reset(); server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> verifier.verify(identity)).isInstanceOfSatisfying(ApiFailure.class, failure -> {
            assertThat(failure.status()).isEqualTo(status); assertThat(failure.code()).isEqualTo(code);
        });
        server.verify();
    }
}
