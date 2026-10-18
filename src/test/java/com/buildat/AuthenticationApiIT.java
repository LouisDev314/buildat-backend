package com.buildat;

import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import com.buildat.auth.*;
import com.buildat.http.ApiFailure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({PostgresTestConfiguration.class, TestAuthConfiguration.class, AuthenticationApiIT.RuntimeConnection.class})
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class AuthenticationApiIT {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class RuntimeConnection {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary
        org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails runtimeDetails(org.testcontainers.postgresql.PostgreSQLContainer postgres) {
            return new org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails() {
                public String getJdbcUrl() { return postgres.getJdbcUrl(); }
                public String getUsername() { return "buildat_test_app"; }
                public String getPassword() { return "test-only"; }
                public String getDriverClassName() { return "org.postgresql.Driver"; }
            };
        }
    }
    @Autowired org.testcontainers.postgresql.PostgreSQLContainer postgres;
    @LocalServerPort int port;
    @Autowired TestJwtProvider provider;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean SupabaseUserVerifier verifier;
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpResponse<String> request(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
    @Test @org.junit.jupiter.api.Order(1)
    void firstRepeatRetrievalIsolationAndFreshRestrictionsUseRealHttpAndPostgres() throws Exception {
        assertThat(jdbc.queryForObject("select current_user", String.class)).isEqualTo("buildat_test_app");
        var subject = UUID.randomUUID(); var token = provider.token(subject);
        assertThat(request("GET", "/api/v1/me", token, null).body()).contains("ACCOUNT_BOOTSTRAP_REQUIRED");
        var created = request("POST", "/api/v1/me/bootstrap", token, "{\"id\":\"" + UUID.randomUUID() + "\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.headers().firstValue("Location")).contains("/api/v1/me");
        var tree = mapper.readTree(created.body()); var id = tree.path("data").path("id").asText();
        assertThat(tree.path("requestId").asText()).isEqualTo(created.headers().firstValue("X-Request-ID").orElseThrow());
        assertThat(request("POST", "/api/v1/me/bootstrap", token, null).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/api/v1/me", token, null).body()).contains(id);
        assertThat(request("HEAD", "/api/v1/me", token, null).body()).isEmpty();
        var other = request("POST", "/api/v1/me/bootstrap", provider.token(UUID.randomUUID()), null);
        assertThat(other.statusCode()).isEqualTo(201); assertThat(other.body()).doesNotContain(id);
        for (String standing : new String[]{"SUSPENDED", "DELETED"}) {
            try (var connection = java.sql.DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                    var statement = connection.prepareStatement("update buildat.accounts set standing=?, updated_at=now() where identity_subject=?")) {
                statement.setString(1, standing); statement.setObject(2, subject); statement.executeUpdate();
            }
            for (var route : new String[]{"/api/v1/me", "/api/v1/me/bootstrap"}) {
                var denied = request(route.endsWith("bootstrap") ? "POST" : "GET", route, token, null);
                assertThat(denied.statusCode()).isEqualTo(403); assertThat(denied.body()).contains("ACCOUNT_RESTRICTED");
            }
        }
        verify(verifier, times(2)).verify(any());
    }
    @Test @org.junit.jupiter.api.Order(1)
    void providerFailureCreatesNothing() throws Exception {
        var subject = UUID.randomUUID();
        doThrow(new ApiFailure(503, "IDENTITY_PROVIDER_UNAVAILABLE")).when(verifier).verify(argThat(identity -> identity.subject().equals(subject)));
        var response = request("POST", "/api/v1/me/bootstrap", provider.token(subject), null);
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(jdbc.queryForObject("select count(*) from buildat.accounts where identity_subject=?", Long.class, subject)).isZero();
    }
    @Test @org.junit.jupiter.api.Order(1)
    void docsAreDisabledByDefaultAndActuatorRemainsUnwrapped() throws Exception {
        assertThat(request("GET", "/v3/api-docs", provider.token(UUID.randomUUID()), null).statusCode()).isEqualTo(403);
        assertThat(mapper.readTree(request("GET", "/actuator/health/liveness", provider.token(UUID.randomUUID()), null).body()))
                .isEqualTo(mapper.readTree("{\"status\":\"UP\"}"));
    }
    @Test @org.junit.jupiter.api.Order(99)
    void databaseOutageFailsClosedForBothAccountEndpoints() throws Exception {
        var token = provider.token(UUID.randomUUID());
        postgres.stop();
        for (String route : new String[]{"/api/v1/me", "/api/v1/me/bootstrap"}) {
            var response = request(route.endsWith("bootstrap") ? "POST" : "GET", route, token, null);
            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).contains("DATABASE_UNAVAILABLE").doesNotContain("jdbc:");
        }
    }

}
