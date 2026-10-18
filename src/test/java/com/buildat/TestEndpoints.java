package com.buildat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;

// Test-only MVC fixtures; no diagnostic application API is shipped.
@TestConfiguration(proxyBeanMethods = false)
public class TestEndpoints {
    @Bean
    @Order(0)
    SecurityFilterChain fixtureSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/test/**")
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable).build();
    }

    @RestController
    @com.buildat.http.ApplicationJsonApi
    public static class Controller {
        public record Input(@Size(min = 3) String name) { }

        @PostMapping("/test/input")
        public Input input(@Valid @RequestBody Input input) { return input; }

        @GetMapping("/test/failure")
        public void failure() { throw new IllegalStateException("secret-database-password"); }

        @GetMapping("/test/servlet-error")
        public void servletError(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
            response.sendError(500, "secret-container-error");
        }

        @GetMapping("/test/wrapped")
        public com.buildat.http.ApiResponse<java.util.Map<String, String>> wrapped(jakarta.servlet.http.HttpServletRequest request) {
            return new com.buildat.http.ApiResponse<>(com.buildat.http.RequestIdFilter.requestId(request), java.util.Map.of("value", "ok"));
        }

        @GetMapping("/test/download")
        public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> download() {
            return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=test.txt")
                    .body(new org.springframework.core.io.ByteArrayResource(new byte[]{1, 2, 3}));
        }

        @GetMapping("/test/empty")
        @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
        public void empty() { }
    }
}
