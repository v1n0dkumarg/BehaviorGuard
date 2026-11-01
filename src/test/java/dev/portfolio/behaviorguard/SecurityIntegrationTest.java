package dev.portfolio.behaviorguard;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(RiskController.class)
@Import(SecurityConfiguration.class)
class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private RiskService service;

    @Test
    void viewerCanReadButOnlyAnalystCanRunDemo() throws Exception {
        when(service.leaderboard()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/risk").with(httpBasic("viewer", "local-viewer-change-me")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/demo/account-takeover").with(httpBasic("viewer", "local-viewer-change-me")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/demo/account-takeover").with(httpBasic("analyst", "local-analyst-change-me")))
                .andExpect(status().isCreated());
    }
}