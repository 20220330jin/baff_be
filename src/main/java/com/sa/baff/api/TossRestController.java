package com.sa.baff.api;

import com.sa.baff.model.vo.TossVO;
import com.sa.baff.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
