package com.sa.baff.api;

import com.sa.baff.config.AccountLinkFeatureProperties;
import com.sa.baff.model.dto.AccountLinkDto;
import com.sa.baff.service.account.AccountLinkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Plan v3 Task 1.5-9 (Plan Review Round 3 P0) 검증 테스트.
 *
 * baff.account-link.enabled 플래그에 따라 Controller가 서비스 호출을 막고 404로 응답하는지 확인.
 */
@ExtendWith(MockitoExtension.class)
class AccountLinkControllerFeatureFlagTest {

    @Mock private AccountLinkService accountLinkService;
    private AccountLinkRestController controller;

    @Test
    @DisplayName("disabled 플래그 시 4개 endpoint 모두 404 ResponseStatusException")
    void when_disabled_allEndpointsReturn404() {
        controller = new AccountLinkRestController(accountLinkService, new AccountLinkFeatureProperties(false));

        assertNotFound(() -> controller.issueToken("social-id"));
        assertNotFound(() -> controller.prepare(new AccountLinkDto.PrepareRequest("t", "ac", "ref")));
        assertNotFound(() -> controller.confirm(new AccountLinkDto.ConfirmRequest("t", "idem", "nonce")));
        assertNotFound(() -> controller.dismissBanner("social-id"));

        verifyNoInteractions(accountLinkService);
    }

    @Test
    @DisplayName("enabled 플래그 시 endpoint가 정상 호출된다")
    void when_enabled_endpointsAccessible() {
        controller = new AccountLinkRestController(accountLinkService, new AccountLinkFeatureProperties(true));

        lenient().when(accountLinkService.issueLinkToken(any()))
                .thenReturn(new AccountLinkDto.IssueTokenResponse("link-token", 300));
        lenient().when(accountLinkService.confirmLink(any()))
                .thenReturn(new AccountLinkDto.ConfirmResponse(true, 1L, LocalDateTime.now()));

        ResponseEntity<?> issueResp = controller.issueToken("social-id");
        assertThat(issueResp.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<?> dismissResp = controller.dismissBanner("social-id");
        assertThat(dismissResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private static void assertNotFound(Runnable r) {
        assertThatThrownBy(r::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
