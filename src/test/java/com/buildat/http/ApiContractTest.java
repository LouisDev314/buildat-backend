package com.buildat.http;

import java.util.UUID;
import com.buildat.account.AccountService;
import com.buildat.account.api.MeController;
import com.buildat.auth.*;
import com.buildat.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MeController.class, excludeAutoConfiguration = org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfiguration.class, SecurityProblems.class, Problems.class, RequestIdFilter.class,
        SuccessResponseAdvice.class, ApiExceptionHandler.class, com.buildat.TestEndpoints.class, com.buildat.TestEndpoints.Controller.class})
class ApiContractTest {
    @MockitoBean AccountService accounts;
    @MockitoBean JwtDecoder decoder;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Test void createdAndExistingAccountHaveExactEnvelopeAndPreserveHeaders() throws Exception {
        var id = UUID.randomUUID(); var view = new AccountService.AccountView(id);
        when(accounts.bootstrap()).thenReturn(new AccountService.BootstrapResult(view, true), new AccountService.BootstrapResult(view, false));
        var first = mvc.perform(post("/api/v1/me/bootstrap").with(jwt()).header("X-Request-ID", "untrusted"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/me"))
                .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse();
        assertThat(mapper.readTree(first.getContentAsString())).isEqualTo(mapper.readTree(
                "{\"requestId\":\"" + first.getHeader("X-Request-ID") + "\",\"data\":{\"id\":\"" + id + "\"}}"));
        assertThat(first.getHeader("X-Request-ID")).isNotEqualTo("untrusted");
        mvc.perform(post("/api/v1/me/bootstrap").with(jwt())).andExpect(status().isOk()).andExpect(header().doesNotExist("Location"));
        when(accounts.requireActiveAccount()).thenReturn(view);
        mvc.perform(head("/api/v1/me").with(jwt())).andExpect(status().isOk()).andExpect(content().string(""));
    }
    @Test void errorsStayProblemDetailsWithMatchingCorrelationAndNoDoubleWrapping() throws Exception {
        when(accounts.requireActiveAccount()).thenThrow(new ApiFailure(403, "ACCOUNT_RESTRICTED"));
        var response = mvc.perform(get("/api/v1/me").with(jwt())).andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("ACCOUNT_RESTRICTED"))
                .andExpect(jsonPath("$.data").doesNotExist()).andReturn().getResponse();
        assertThat(mapper.readTree(response.getContentAsString()).path("requestId").asText()).isEqualTo(response.getHeader("X-Request-ID"));
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("secret JDBC")).when(accounts).requireActiveAccount();
        mvc.perform(get("/api/v1/me").with(jwt())).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }
    @Test void alreadyWrappedEmptyAndDownloadResponsesKeepTheirRepresentations() throws Exception {
        mvc.perform(get("/test/wrapped")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("ok")).andExpect(jsonPath("$.data.data").doesNotExist());
        mvc.perform(get("/test/empty")).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/test/download")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=test.txt"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

}
