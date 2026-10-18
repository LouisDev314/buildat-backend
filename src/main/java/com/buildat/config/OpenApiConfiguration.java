package com.buildat.config;

import com.buildat.http.ApplicationJsonApi;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.AnnotatedElementUtils;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean OpenAPI buildatOpenApi() {
        return new OpenAPI().info(new Info().title("Buildat API").version("v1"))
                .components(new Components().addSecuritySchemes("supabaseBearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean OperationCustomizer applicationContract() {
        return (operation, handler) -> {
            if (!AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), ApplicationJsonApi.class)) return operation;
            operation.getResponses().forEach((status, response) -> {
                if (!status.startsWith("2") || response.getContent() == null) return;
                response.getContent().forEach((type, media) -> {
                    if (!type.equals("application/json")) return;
                    var data = media.getSchema();
                    media.setSchema(new ObjectSchema().addProperty("requestId", new StringSchema().format("uuid"))
                            .addProperty("data", data).required(java.util.List.of("requestId", "data")));
                });
                response.addHeaderObject("X-Request-ID", new Header().schema(new StringSchema().format("uuid")));
                if (status.equals("201")) response.addHeaderObject("Location", new Header().schema(new StringSchema()).description("/api/v1/me"));
            });
            var problem = new ObjectSchema()
                    .addProperty("type", new StringSchema().format("uri"))
                    .addProperty("title", new StringSchema())
                    .addProperty("status", new IntegerSchema())
                    .addProperty("detail", new StringSchema())
                    .addProperty("instance", new StringSchema().format("uri-reference"))
                    .addProperty("code", new StringSchema())
                    .addProperty("requestId", new StringSchema().format("uuid"))
                    .required(java.util.List.of("type", "title", "status", "detail", "instance", "code", "requestId"));
            var errors = new java.util.LinkedHashMap<String, String>();
            errors.put("401", "AUTHENTICATION_REQUIRED, INVALID_TOKEN");
            errors.put("403", "ANONYMOUS_IDENTITY_NOT_ALLOWED, ACCOUNT_RESTRICTED, "
                    + (handler.getMethod().getName().equals("bootstrap") ? "EMAIL_CONFIRMATION_REQUIRED" : "ACCOUNT_BOOTSTRAP_REQUIRED"));
            errors.put("503", "DATABASE_UNAVAILABLE, IDENTITY_PROVIDER_UNAVAILABLE");
            errors.put("500", "INTERNAL_ERROR");
            if (handler.getMethod().getName().equals("bootstrap")) errors.put("409", "ACCOUNT_STATE_CONFLICT");
            errors.forEach((status, codes) -> operation.getResponses().addApiResponse(status,
                    new ApiResponse().description(codes).content(new Content().addMediaType("application/problem+json",
                            new MediaType().schema(problem)))
                            .addHeaderObject("X-Request-ID", new Header().schema(new StringSchema().format("uuid")))));
            return operation;
        };
    }
}
