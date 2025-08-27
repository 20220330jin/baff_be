package com.sa.baff.api;

import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *
 *
 * @author hjkim
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;

    /**
     * 현재 로그인된 사용자의 정보를 조회하는 API입니다.
     * 프론트엔드에서는 이 API를 호출하여 사용자의 로그인 상태를 확인합니다.
     * 요청 헤더의 쿠키에 담긴 JWT가 유효하면, Spring Security와 JwtAuthenticationFilter가
     * 사용자 ID를 @AuthenticationPrincipal에 주입해줍니다.
     * JWT가 없거나 유효하지 않으면, @AuthenticationPrincipal은 null이 됩니다.
     *
     * @param userId @AuthenticationPrincipal을 통해 JWT 토큰에서 추출된 사용자 ID
     * @return 로그인된 사용자의 정보 (ID, 이메일, 닉네임, 프로필 이미지) 또는 null (로그인되지 않은 경우)
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyInfo(@AuthenticationPrincipal String userId) {
        // userId가 null이면 로그인되지 않은 상태이므로, 사용자 정보를 반환하지 않습니다.
        System.out.println("=================FETCHME" + userId);
        if (userId == null) {
            return ResponseEntity.ok(null);
        }
        UserDto userInfo = userService.getUserInfo(userId);
        System.out.println("USERINFO=======" + userInfo);
        return ResponseEntity.ok(userInfo);
    }

    /**
     * 어드민 대시보드 유저리스트 조회 api
     */
    @GetMapping("/getUserList")
    public List<UserBDto.getUserList> getUserList() {
        return userService.getUserList();
    }
}
