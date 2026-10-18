package com.buildat.http;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class SuccessResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
        return AnnotatedElementUtils.hasAnnotation(method.getContainingClass(), ApplicationJsonApi.class);
    }
    @Override public Object beforeBodyWrite(Object body, MethodParameter method, MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !(response instanceof ServletServerHttpResponse servletResponse)) return body;
        if (request.getMethod() == HttpMethod.HEAD) return null;
        int status = servletResponse.getServletResponse().getStatus();
        if (body == null || status < 200 || status >= 300 || status == 204
                || request.getMethod() == HttpMethod.HEAD || body instanceof ApiResponse<?>
                || body instanceof ProblemDetail || body instanceof String || body instanceof byte[]
                || body instanceof org.springframework.core.io.Resource
                || !MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) return body;
        return new ApiResponse<>(RequestIdFilter.requestId(servletRequest.getServletRequest()), body);
    }
}
