package com.sa.baff.api;

import com.sa.baff.config.AccountLinkFeatureProperties;
import com.sa.baff.model.dto.AccountLinkDto;
import com.sa.baff.service.account.AccountLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 계정 통합 API (spec §4.1~§4.3).
 *
 *  - POST /api/account/link/issue-token : 웹/앱에서 호출 (로그인 세션 필수)
 *  - POST /api/account/link/prepare     : 토스 미니앱에서 호출
 *  - POST /api/account/link/confirm     : 토스 미니앱에서 호출
 *
 * Plan v3 Task 1.5-9 (Plan Review Round 3 P0):
 *   baff.account-link.enabled=false (기본값) 일 때 전체 endpoint가 404로 응답.
 *   Phase 2 FE + CP2-FE + 내부 테스트 + 대표님 릴리즈 승인 후에만 true로 전환.
 */
@RestController
@RequestMapping("/api/account/link")
@RequiredArgsConstructor
public class AccountLinkRestController {

    private final AccountLinkService accountLinkService;
    private final AccountLinkFeatureProperties featureProperties;

    private void ensureEnabled() {
        if (!featureProperties.enabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/issue-token")
    public ResponseEntity<?> issueToken(@AuthenticationPrincipal String socialId) {
        ensureEnabled();
        try {
            return ResponseEntity.ok(accountLinkService.issueLinkToken(socialId));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new AccountLinkDto.ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/prepare")
    public ResponseEntity<?> prepare(@RequestBody AccountLinkDto.PrepareRequest request) {
        ensureEnabled();
        try {
            return ResponseEntity.ok(accountLinkService.prepareLink(request));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new AccountLinkDto.ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody AccountLinkDto.ConfirmRequest request) {
        ensureEnabled();
        try {
            return ResponseEntity.ok(accountLinkService.confirmLink(request));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new AccountLinkDto.ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/dismiss-banner")
    public ResponseEntity<?> dismissBanner(@AuthenticationPrincipal String socialId) {
        ensureEnabled();
        try {
            accountLinkService.dismissLinkBanner(socialId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new AccountLinkDto.ErrorResponse(e.getMessage()));
        }
    }
}
