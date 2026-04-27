package com.sa.baff.api;

import com.sa.baff.model.vo.TossVO;
import com.sa.baff.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/toss")
@RequiredArgsConstructor
public class TossRestController {

    private final UserService userService;

    @Value("${toss.unlink.username:}")
    private String unlinkUsername;

    @Value("${toss.unlink.password:}")
    private String unlinkPassword;

    @PostMapping("/login")
    public ResponseEntity<String> handleTossLogin(@RequestBody TossVO.LoginRequest request,
                                                   HttpServletRequest httpRequest) {
        String jwt = userService.loginWithToss(request, httpRequest);
        return ResponseEntity.ok(jwt);
    }

    @PostMapping("/unlink-callback")
    public ResponseEntity<Void> handleUnlinkCallback(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody TossVO.UnlinkCallback callbackData) {
        if (!verifyBasicAuth(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.unlinkTossAccount(callbackData.getUserKey());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * 토스 unlink 콜백 GET 처리.
     * 콘솔에서 GET 방식 콜백을 등록한 경우 토스 서버가 query parameter로 호출.
     * referrer: UNLINK | WITHDRAWAL_TERMS | WITHDRAWAL_TOSS
     */
    @GetMapping("/unlink-callback")
    public ResponseEntity<Void> handleUnlinkCallbackGet(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String userKey,
            @RequestParam(required = false) String referrer) {
        if (!verifyBasicAuth(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.unlinkTossAccount(userKey);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * 토스 콘솔에 등록한 Basic Auth 헤더 검증.
     * 토스가 보내는 헤더: Authorization: Basic base64(username:password)
     * username/password가 미설정이면(빈 문자열) 검증을 스킵 — 로컬 개발용.
     */
    private boolean verifyBasicAuth(String authHeader) {
        if (unlinkUsername == null || unlinkUsername.isBlank()
                || unlinkPassword == null || unlinkPassword.isBlank()) {
            log.warn("[Toss unlink] Basic Auth 자격증명 미설정 — 검증 스킵 (운영에서는 반드시 설정 필요)");
            return true;
        }
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("[Toss unlink] Authorization 헤더 누락/형식 오류");
            return false;
        }
        try {
            String encoded = authHeader.substring("Basic ".length()).trim();
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            String expected = unlinkUsername + ":" + unlinkPassword;
            boolean match = constantTimeEquals(decoded, expected);
            if (!match) {
                log.warn("[Toss unlink] Basic Auth 자격증명 불일치");
            }
            return match;
        } catch (IllegalArgumentException e) {
            log.warn("[Toss unlink] Authorization 헤더 base64 디코딩 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 타이밍 공격 회피용 상수시간 비교.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ab.length != bb.length) return false;
        int result = 0;
        for (int i = 0; i < ab.length; i++) {
            result |= ab[i] ^ bb[i];
        }
        return result == 0;
    }
}
