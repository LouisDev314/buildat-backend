package com.buildat.http;

import com.buildat.TestEndpoints;
import com.buildat.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TestEndpoints.Controller.class, excludeAutoConfiguration =
        org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfiguration.class, Problems.class, RequestIdFilter.class,
        ApiExceptionHandler.class, ProblemErrorController.class, TestEndpoints.class})
class HttpFoundationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void anonymousRoutesAreDeniedWithoutLoginOrSession() throws Exception {
        for (var path : new String[]{"/api/v1/me", "/login", "/actuator", "/actuator/health",
                "/actuator/env", "/actuator/health/readiness/db", "/error"}) {
            var result = mvc.perform(get(path).header(RequestIdFilter.HEADER, "untrusted"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(header().doesNotExist("Location"))
                    .andExpect(header().doesNotExist("Set-Cookie"))
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                    .andReturn();
            var body = mapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.path("requestId").asText()).isEqualTo(result.getResponse().getHeader(RequestIdFilter.HEADER));
            assertThat(body.path("requestId").asText()).isNotEqualTo("untrusted");
        }
        mvc.perform(post("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", "Basic dGVzdDp0ZXN0"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/actuator/health/liveness")).andExpect(status().isUnauthorized());
    }

    @Test
    void firewallRejectionsUseTheSameProblemContract() throws Exception {
        mvc.perform(get("/api/;private")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(header().exists(RequestIdFilter.HEADER));
    }

    @Test
    void servletErrorDispatchIsGenericAndReusesCorrelation() throws Exception {
        mvc.perform(get("/error").with(request -> { request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR); return request; })
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, "/failed")
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_MESSAGE, "secret"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.instance").value("/failed"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @Test
    void evenAuthenticatedUsersHaveNoImplicitPrivileges() throws Exception {
        mvc.perform(get("/api/v1/me").with(user("builder")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void mvcErrorsKeepProtocolHeadersAndNeverEchoInputOrExceptionText() throws Exception {
        mvc.perform(post("/test/input?token=private").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.instance").value("/test/input"));
        mvc.perform(post("/test/input").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
        mvc.perform(get("/test/input")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"));
        mvc.perform(post("/test/input").contentType(MediaType.TEXT_PLAIN).content("secret"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/test/failure")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
        mvc.perform(get("/test/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/test/empty")).andExpect(status().isNoContent())
                .andExpect(content().string("")).andExpect(header().exists(RequestIdFilter.HEADER));
    }
}
