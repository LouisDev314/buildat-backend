package com.buildat;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        // User verified the designated hosted database as PostgreSQL 17.11 on 2026-10-18.
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17.11"))
                .withInitScript("database-test-roles.sql");
    }
}
