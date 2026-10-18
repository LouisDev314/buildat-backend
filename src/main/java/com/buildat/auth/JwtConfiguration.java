package com.buildat.auth;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SupabaseProperties.class)
public class JwtConfiguration {
    @Bean
    JwtDecoder jwtDecoder(SupabaseProperties properties) {
        return createDecoder(properties.jwksUri(), properties.issuer().toString(), properties.algorithms(), properties.audience());
    }

    static JwtDecoder createDecoder(java.net.URI jwks, String issuer, java.util.Set<String> allowedAlgorithms, String audience) {
        var factory = ProviderHttp.requestFactory();
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks.toString())
                .restOperations(new RestTemplate(factory))
                .jwsAlgorithms(algorithms -> {
                    algorithms.clear();
                    allowedAlgorithms.forEach(name -> algorithms.add(SignatureAlgorithm.from(name)));
                }).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new IdentityClaimsValidator(Clock.systemUTC()),
                jwt -> jwt.getAudience().contains(audience)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null))));
        return decoder;
    }
}
