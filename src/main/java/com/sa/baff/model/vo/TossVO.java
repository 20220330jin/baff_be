package com.sa.baff.model.vo;

import lombok.Getter;

public class TossVO {

    @Getter
    public static class LoginRequest {
        private String authorizationCode;
        private String referrer;
    }

    @Getter
    public static class UnlinkCallback {
        private String userKey;
        private String referrer;
    }
}
