package com.sa.baff.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserVO {

    @Getter
    @NoArgsConstructor
    public static class editNicknameRequest {
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    public static class editProfileImage {
        private String imageUrl;
    }
}
