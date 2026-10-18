package com.buildat.http;

import com.buildat.account.AccountService;
import com.buildat.account.api.MeController;
import com.buildat.auth.SecurityProblems;
import com.buildat.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MeController.class, properties = "buildat.docs.enabled=true",
        excludeAutoConfiguration = org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class)
@ActiveProfiles({"local-development", "production"})
@Import({SecurityConfiguration.class, SecurityProblems.class, Problems.class, RequestIdFilter.class})
class ProductionDocumentationTest {
    @MockitoBean AccountService accounts;
    @MockitoBean JwtDecoder decoder;
    @Autowired MockMvc mvc;
    @Test void productionNeverUsesTheLocalDocumentationAllowlist() throws Exception {
        for (String path : new String[]{"/v3/api-docs", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/index.html"}) {
            mvc.perform(get(path).with(jwt())).andExpect(status().isForbidden());
        }
    }
}
