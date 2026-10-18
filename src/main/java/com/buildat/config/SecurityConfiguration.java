package com.buildat.config;

import com.buildat.http.Problems;
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
    SecurityFilterChain securityFilterChain(HttpSecurity http, Problems problems) throws Exception {
        return http
                .authorizeHttpRequests(rules -> rules
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .anyRequest().denyAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> problems.write(request, response, 401))
                        .accessDeniedHandler((request, response, exception) -> problems.write(request, response, 403)))
                .build();
    }

    @Bean
    RequestRejectedHandler requestRejectedHandler(Problems problems) {
        return (request, response, exception) -> problems.write(request, response, 400);
    }
}
