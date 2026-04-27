package com.sa.baff.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sa.baff.util.TossSocialIdMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossAuthServiceImpl implements TossAuthService {

    @Autowired(required = false)
    @Qualifier("tossWebClient")
    private WebClient tossWebClient;

    @Value("${toss.decrypt.key:}")
    private String tossDecryptKey;

    @Value("${toss.decrypt.aad:}")
    private String tossAad;

    @Override
    public TossUserKeyResult resolveTossUserKey(String authorizationCode, String referrer) {
        if (tossWebClient == null) {
            throw new IllegalStateException("Toss 연동이 설정되지 않았습니다.");
        }

        long t0 = System.currentTimeMillis();
        TokenResponse tokenResponse = getAccessToken(authorizationCode, referrer);
        long t1 = System.currentTimeMillis();
        UserInfo encryptedUserInfo = getUserInfoFromToss(tokenResponse.getSuccess().getAccessToken());
        long t2 = System.currentTimeMillis();

        String decryptedName = decryptField(encryptedUserInfo.getName());
        String decryptedEmail = decryptField(encryptedUserInfo.getEmail());
        long t3 = System.currentTimeMillis();

        log.info("[TIMING] resolveTossUserKey getToken={}ms getUserInfo={}ms decrypt={}ms total={}ms",
                t1 - t0, t2 - t1, t3 - t2, t3 - t0);
        log.info("Toss auth resolve - userKey: {}, name: {}", encryptedUserInfo.getUserKey(), decryptedName);

        String tossUserKey = TossSocialIdMapper.toStoredSocialId(encryptedUserInfo.getUserKey());
        return new TossUserKeyResult(tossUserKey, encryptedUserInfo.getUserKey(), decryptedEmail, decryptedName);
    }

    private TokenResponse getAccessToken(String authorizationCode, String referrer) {
        log.info("Toss token 요청 - authorizationCode: {}", authorizationCode);

        try {
            String rawResponse = tossWebClient.post()
                    .uri("/api-partner/v1/apps-in-toss/user/oauth2/generate-token")
                    .bodyValue(new TokenGenerationRequest(authorizationCode, referrer))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Toss token 응답: {}", rawResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            TokenResponse tokenResponse = objectMapper.readValue(rawResponse, TokenResponse.class);

            if (!"SUCCESS".equals(tokenResponse.getResultType())) {
                String errorDetail = tokenResponse.getError() != null
                        ? tokenResponse.getError().getErrorCode() + ": " + tokenResponse.getError().getReason()
                        : "unknown";
                log.error("Toss token 발급 실패: resultType={}, error={}", tokenResponse.getResultType(), errorDetail);
                throw new RuntimeException("Toss token 발급 실패: " + errorDetail);
            }

            return tokenResponse;

        } catch (WebClientResponseException e) {
            log.error("Toss token API 에러: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Toss token API error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Toss token 요청 중 에러: {}", e.getMessage(), e);
            throw new RuntimeException("Toss token generation failed", e);
        }
    }

    private UserInfo getUserInfoFromToss(String accessToken) {
        log.info("Toss 사용자 정보 요청");

        try {
            String rawResponse = tossWebClient.get()
                    .uri("/api-partner/v1/apps-in-toss/user/oauth2/login-me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Toss 사용자 정보 응답: {}", rawResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            TossUserInfoResponse response = objectMapper.readValue(rawResponse, TossUserInfoResponse.class);

            if (response != null && response.getSuccess() != null) {
                return response.getSuccess();
            }
            throw new RuntimeException("Toss user info response is null");

        } catch (WebClientResponseException e) {
            log.error("Toss user info API 에러: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Toss user info API error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Toss 사용자 정보 요청 중 에러: {}", e.getMessage(), e);
            throw new RuntimeException("Toss user info retrieval failed", e);
        }
    }

    private String decryptField(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return null;
        }

        try {
            final int IV_LENGTH = 12;
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] keyBytes = Base64.getDecoder().decode(tossDecryptKey);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);

            GCMParameterSpec nonceSpec = new GCMParameterSpec(16 * Byte.SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, nonceSpec);
            cipher.updateAAD(tossAad.getBytes());

            byte[] decrypted = cipher.doFinal(decoded, IV_LENGTH, decoded.length - IV_LENGTH);
            return new String(decrypted);

        } catch (Exception e) {
            log.error("Toss 복호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to decrypt Toss user info", e);
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class TokenGenerationRequest {
        private final String authorizationCode;
        private final String referrer;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        private String resultType;
        private SuccessResponse success;
        private ErrorResponse error;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ErrorResponse {
        private String errorCode;
        private String reason;
    }

    @Getter
    private static class SuccessResponse {
        private String accessToken;
        private String refreshToken;
        private String expiresIn;
        private String tokenType;
        private String scope;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TossUserInfoResponse {
        private String resultType;
        private UserInfo success;
        private ErrorResponse error;
    }

    @Getter
    @Setter
    static class UserInfo {
        private Long userKey;
        private String scope;
        private String[] agreedTerms;
        private String policy;
        private String certTxId;
        private String name;
        private String callingCode;
        private String phone;
        private String birthday;
        private String ci;
        private String di;
        private String gender;
        private String nationality;
        private String email;
    }
}
