package com.buildat.auth;

import java.time.Instant;
import com.buildat.http.ApiFailure;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

@Component
public class SupabaseUserVerifier {
    private final RestClient client;
    @org.springframework.beans.factory.annotation.Autowired
    public SupabaseUserVerifier(SupabaseProperties properties) {
        var factory = ProviderHttp.requestFactory();
        client = RestClient.builder().requestFactory(factory)
                .baseUrl(properties.projectUrl().toString())
                .defaultHeader("apikey", properties.publishableKey()).build();
    }

    SupabaseUserVerifier(RestClient client) { this.client = client; }

    public void verify(CurrentIdentity.Identity identity) {
        try {
            var user = client.get().uri("/auth/v1/user")
                    .headers(headers -> headers.setBearerAuth(identity.accessToken()))
                    .retrieve().body(JsonNode.class);
            if (user == null || !user.path("id").isString()
                    || !identity.subject().toString().equals(user.path("id").asString())) {
                throw new ApiFailure(503, "IDENTITY_PROVIDER_UNAVAILABLE");
            }
            if (!user.path("is_anonymous").isBoolean()) {
                throw new ApiFailure(503, "IDENTITY_PROVIDER_UNAVAILABLE");
            }
            if (user.path("is_anonymous").asBoolean()) {
                throw new ApiFailure(403, "ANONYMOUS_IDENTITY_NOT_ALLOWED");
            }
            if (!user.path("email").isString() || user.path("email").asString().isBlank()
                    || user.path("email_confirmed_at").isNull() || user.path("email_confirmed_at").isMissingNode()) {
                throw new ApiFailure(403, "EMAIL_CONFIRMATION_REQUIRED");
            }
            if (!user.path("email_confirmed_at").isString()) {
                throw new ApiFailure(503, "IDENTITY_PROVIDER_UNAVAILABLE");
            }
            Instant.parse(user.path("email_confirmed_at").asString());
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 404) {
                throw new ApiFailure(401, "INVALID_TOKEN");
            }
            throw new ApiFailure(503, "IDENTITY_PROVIDER_UNAVAILABLE");
        } catch (RestClientException | java.time.format.DateTimeParseException exception) {
            throw new ApiFailure(503, "IDENTITY_PROVIDER_UNAVAILABLE");
        }
    }
}
