package com.sa.baff.api;

import com.sa.baff.model.dto.AccountLinkDto;
import com.sa.baff.service.account.AccountLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 계정 통합 API (spec §4.1~§4.3).
 *
 *  - POST /api/account/link/issue-token : 웹/앱에서 호출 (로그인 세션 필수)
 *  - POST /api/account/link/prepare     : 토스 미니앱에서 호출
 *  - POST /api/account/link/confirm     : 토스 미니앱에서 호출
 */
@RestController
@RequestMapping("/api/account/link")
@RequiredArgsConstructor
public class AccountLinkRestController {

    private final AccountLinkService accountLinkService;

    @PostMapping("/issue-token")
    public ResponseEntity<?> issueToken(@AuthenticationPrincipal String socialId) {
        try {
            return ResponseEntity.ok(accountLinkService.issueLinkToken(socialId));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new AccountLinkDto.ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/prepare")
    public ResponseEntity<?> prepare(@RequestBody AccountLinkDto.PrepareRequest request) {
        try {
            return ResponseEntity.ok(accountLinkService.prepareLink(request));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new AccountLinkDto.ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody AccountLinkDto.ConfirmRequest request) {
        try {
            return ResponseEntity.ok(accountLinkService.confirmLink(request));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new AccountLinkDto.ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/dismiss-banner")
    public ResponseEntity<?> dismissBanner(@AuthenticationPrincipal String socialId) {
        try {
            accountLinkService.dismissLinkBanner(socialId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new AccountLinkDto.ErrorResponse(e.getMessage()));
        }
    }
}
