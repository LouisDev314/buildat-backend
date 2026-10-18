package com.buildat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.buildat.http.RequestIdFilter;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({PostgresTestConfiguration.class, TestEndpoints.class, TestEndpoints.Controller.class})
class BuildatServerApplicationIT {
    @LocalServerPort int port;
    @Autowired Flyway flyway;
    @Autowired org.testcontainers.postgresql.PostgreSQLContainer postgres;
    @Autowired EntityManagerFactory entities;
    @Autowired ConfigurableApplicationContext context;
    @Autowired Environment environment;
    @Autowired ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header(RequestIdFilter.HEADER, "caller-controlled").GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void migrationIsAppliedOnceAndHibernateRemainsValidationOnly() {
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.info().applied()[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(entities.getMetamodel().getEntities()).isEmpty();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void actualHttpProbesExposeOnlyStatusAndReadinessCanRefuseTraffic() throws Exception {
        var live = get("/actuator/health/liveness");
        assertThat(live.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(live.body())).isEqualTo(mapper.readTree("{\"status\":\"UP\"}"));
        assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(200);
        try {
            AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
            var unavailable = get("/actuator/health/readiness");
            assertThat(unavailable.statusCode()).isEqualTo(503);
            assertThat(mapper.readTree(unavailable.body())).isEqualTo(mapper.readTree("{\"status\":\"OUT_OF_SERVICE\"}"));
            assertThat(get("/actuator/health/liveness").statusCode()).isEqualTo(200);
        } finally {
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    @Test
    @org.junit.jupiter.api.Order(99)
    void databaseOutageMakesReadinessFailWithoutAffectingLiveness() throws Exception {
        postgres.stop();
        var ready = get("/actuator/health/readiness");
        assertThat(ready.statusCode()).isEqualTo(503);
        assertThat(mapper.readTree(ready.body())).isEqualTo(mapper.readTree("{\"status\":\"DOWN\"}"));
        assertThat(get("/actuator/health/liveness").statusCode()).isEqualTo(200);
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void actualHttpErrorsAreSafeAndCorrelated() throws Exception {
        var first = get("/api/v1/me?token=private");
        assertThat(first.statusCode()).isEqualTo(401);
        assertThat(first.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        var body = mapper.readTree(first.body());
        var id = first.headers().firstValue(RequestIdFilter.HEADER).orElseThrow();
        assertThat(body.path("requestId").asText()).isEqualTo(id).isNotEqualTo("caller-controlled");
        assertThat(body.path("instance").asText()).isEqualTo("/api/v1/me");
        assertThat(get("/api/v1/me").headers().firstValue(RequestIdFilter.HEADER).orElseThrow()).isNotEqualTo(id);
        for (var path : new String[]{"/login", "/actuator/health", "/actuator/env"}) {
            assertThat(get(path).statusCode()).isEqualTo(401);
        }
        var failure = get("/test/failure");
        assertThat(failure.statusCode()).isEqualTo(500);
        assertThat(failure.body()).doesNotContain("secret-database-password");
        assertThat(mapper.readTree(failure.body()).path("requestId").asText())
                .isEqualTo(failure.headers().firstValue(RequestIdFilter.HEADER).orElseThrow());
        var servletError = get("/test/servlet-error?token=private");
        assertThat(servletError.statusCode()).isEqualTo(500);
        assertThat(servletError.body()).doesNotContain("secret-container-error", "private");
        assertThat(mapper.readTree(servletError.body()).path("requestId").asText())
                .isEqualTo(servletError.headers().firstValue(RequestIdFilter.HEADER).orElseThrow());
        assertThat(mapper.readTree(servletError.body()).path("instance").asText()).isEqualTo("/test/servlet-error");
        var head = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health/liveness"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(head.statusCode()).isEqualTo(200);
        assertThat(head.body()).isEmpty();
        assertThat(head.headers().firstValue(RequestIdFilter.HEADER)).isPresent();
        var method = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/test/input"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(method.statusCode()).isEqualTo(405);
        assertThat(method.headers().firstValue("Allow").orElseThrow()).contains("POST");
    }
}
