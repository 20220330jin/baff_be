package com.sa.baff.api;

import com.sa.baff.model.vo.TossVO;
import com.sa.baff.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/toss")
@RequiredArgsConstructor
public class TossRestController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<String> handleTossLogin(@RequestBody TossVO.LoginRequest request,
                                                   HttpServletRequest httpRequest) {
        String jwt = userService.loginWithToss(request, httpRequest);
        return ResponseEntity.ok(jwt);
    }

    @PostMapping("/unlink-callback")
    public ResponseEntity<Void> handleUnlinkCallback(@RequestBody TossVO.UnlinkCallback callbackData) {
        userService.unlinkTossAccount(callbackData.getUserKey());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * 토스 unlink 콜백 GET 처리.
     * 콘솔에서 GET 방식 콜백을 등록한 경우 토스 서버가 query parameter로 호출.
     * referrer: UNLINK | WITHDRAWAL_TERMS | WITHDRAWAL_TOSS
     */
    @GetMapping("/unlink-callback")
    public ResponseEntity<Void> handleUnlinkCallbackGet(@RequestParam String userKey,
                                                        @RequestParam(required = false) String referrer) {
        userService.unlinkTossAccount(userKey);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
