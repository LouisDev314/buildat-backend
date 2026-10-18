package com.buildat.config;

import com.buildat.http.Problems;
import com.buildat.auth.SecurityProblems;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.RequestRejectedHandler;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Problems problems, SecurityProblems securityProblems, Environment environment) throws Exception {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> java.util.List.of());
        boolean docs = environment.getProperty("buildat.docs.enabled", Boolean.class, false)
                && environment.acceptsProfiles(org.springframework.core.env.Profiles.of("local-development"))
                && !environment.acceptsProfiles(org.springframework.core.env.Profiles.of("production"));
        return http
                .authorizeHttpRequests(rules -> rules
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/me/bootstrap").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/me").authenticated()
                        .requestMatchers(HttpMethod.HEAD, "/api/v1/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml")
                            .access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(docs))
                        .requestMatchers(HttpMethod.HEAD, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml")
                            .access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(docs))
                        .anyRequest().denyAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(securityProblems)
                        .accessDeniedHandler((request, response, exception) -> problems.write(request, response, 403)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(securityProblems)
                        .accessDeniedHandler((request, response, exception) -> problems.write(request, response, 403)))
                .build();
    }

    @Bean
    RequestRejectedHandler requestRejectedHandler(Problems problems) {
        return (request, response, exception) -> problems.write(request, response, 400);
    }
}
