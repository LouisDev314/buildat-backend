package com.buildat.auth;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration(proxyBeanMethods = false)
public class TestAuthConfiguration {
    @Bean(destroyMethod = "close") TestJwtProvider testJwtProvider() throws Exception { return new TestJwtProvider(); }
    @Bean @Primary JwtDecoder testDecoder(TestJwtProvider provider) {
        return JwtConfiguration.createDecoder(provider.uri(), TestJwtProvider.ISSUER, java.util.Set.of("RS256"), "authenticated");
    }
}
