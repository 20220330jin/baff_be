package com.sa.baff.api;

import com.sa.baff.config.AccountLinkFeatureProperties;
import com.sa.baff.service.account.AccountLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Plan v3 Task 1.5-9 — CP2 Round 3 P2 보강.
 *
 * MockMvc로 HTTP 레이어에서도 disabled 상태 404 응답 확인.
 * @RequestBody deserialization이 있는 prepare/confirm에서도 service 미호출 검증.
 */
@ExtendWith(MockitoExtension.class)
class AccountLinkControllerFeatureFlagMvcTest {

    @Mock private AccountLinkService accountLinkService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AccountLinkRestController controller = new AccountLinkRestController(
                accountLinkService, new AccountLinkFeatureProperties(false));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("MVC: disabled + body 있는 요청 (prepare) → 404 + service 미호출")
    void disabled_prepareWithBody_returns404_noServiceCall() throws Exception {
        String body = """
                {"linkToken":"x","authorizationCode":"x","referrer":"x"}
                """;

        mockMvc.perform(post("/api/account/link/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        verifyNoInteractions(accountLinkService);
    }

    @Test
    @DisplayName("MVC: disabled + body 있는 요청 (confirm) → 404 + service 미호출")
    void disabled_confirmWithBody_returns404_noServiceCall() throws Exception {
        String body = """
                {"linkToken":"x","idempotencyKey":"x","nonce":"x"}
                """;

        mockMvc.perform(post("/api/account/link/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        verifyNoInteractions(accountLinkService);
    }

    @Test
    @DisplayName("MVC: disabled + issue-token → 404")
    void disabled_issueToken_returns404() throws Exception {
        mockMvc.perform(post("/api/account/link/issue-token"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(accountLinkService);
    }

    @Test
    @DisplayName("MVC: disabled + dismiss-banner → 404")
    void disabled_dismissBanner_returns404() throws Exception {
        mockMvc.perform(patch("/api/account/link/dismiss-banner"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(accountLinkService);
    }
}
