package com.buildat;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.url=jdbc:postgresql://127.0.0.1:1/unused", "spring.flyway.user=unused", "spring.flyway.password=unused",
        "spring.flyway.default-schema=public"})
@ActiveProfiles({"local-development", "test"})
@Import(PostgresTestConfiguration.class)
class OpenApiContractIT {
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
    @Test void generatedSpecDescribesRuntimeEnvelopeErrorsAndBearerSecurity() throws Exception {
        var response = get("/v3/api-docs"); assertThat(response.statusCode()).isEqualTo(200);
        var spec = mapper.readTree(response.body());
        var scheme = spec.path("components").path("securitySchemes").path("supabaseBearer");
        assertThat(scheme.path("scheme").asText()).isEqualTo("bearer");
        assertThat(scheme.path("bearerFormat").asText()).isEqualTo("JWT");
        var bootstrap = spec.path("paths").path("/api/v1/me/bootstrap").path("post");
        assertThat(bootstrap.has("requestBody")).isFalse();
        for (String status : new String[]{"200", "201"}) {
            var schema = bootstrap.path("responses").path(status).path("content").path("application/json").path("schema");
            assertThat(schema.path("properties").has("requestId")).isTrue();
            assertThat(schema.path("properties").path("data").path("$ref").asText()).endsWith("/AccountDto");
        }
        assertThat(bootstrap.path("responses").path("201").path("headers").has("Location")).isTrue();
        for (String status : new String[]{"401", "403", "409", "503"}) {
            var problem = bootstrap.path("responses").path(status).path("content").path("application/problem+json").path("schema");
            assertThat(problem.path("properties").has("code")).isTrue();
            assertThat(problem.path("properties").has("requestId")).isTrue();
        }
        assertThat(spec.path("components").path("schemas").path("AccountDto").path("properties").size()).isEqualTo(1);
        assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(200);
        assertThat(get("/actuator/health/liveness").body()).isEqualTo("{\"status\":\"UP\"}");
    }
}
