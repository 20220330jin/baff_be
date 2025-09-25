package com.sa.baff.model.dto;

import com.sa.baff.domain.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 유저 관련 DTO
 */
public class UserBDto {
    /**
     * 어드민 대시보드 유저리스트 조회 DTO
     */
    @Setter
    @Getter
    public static class getUserList {
        /* 유저ID */
        private Long userId;
        /* 유저 닉네임 */
        private String nickname;
        /* 유저 이메일 */
        private String email;
        /* 유저 이미지 url */
        private String userProfileUrl;
        /* 유저 등록시간 */
        private LocalDateTime regDateTime;
        /* 유저 권한 */
        private Role role;
        /* 유저 상태 */
        private String status;
        /* Platform */
        private String platform;
        /* Provider */
        private String provider;
    }

    /**
     * 유저 정보 DTO
     */
    @Getter
    public static class getUserInfo {
        /* 유저ID */
        private Long userId;
        /* 유저 닉네임 */
        private String nickname;
        /* 유저 이메일 */
        private String email;
        /* 유저 이미지 url */
        private String userProfileUrl;
        /* 유저 등록 시간 */
        private LocalDateTime regDateTime;
        /* 가입 */
        private String provider;

        public getUserInfo(Long userId, String nickname, String email, String userProfileUrl, LocalDateTime regDateTime, String provider) {
            this.userId = userId;
            this.nickname = nickname;
            this.email = email;
            this.userProfileUrl = userProfileUrl;
            this.regDateTime = regDateTime;
            this.provider = provider;

        }
    }
}
