package com.sa.baff.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
public class TossPromotionApiClient {

    private static final String GET_KEY_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion/get-key";
    private static final String EXECUTE_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion";
    private static final String RESULT_PATH = "/api-partner/v1/apps-in-toss/promotion/execution-result";
    private static final String USER_KEY_HEADER = "x-toss-user-key";

    private WebClient tossWebClient;

    @Autowired(required = false)
    public void setTossWebClient(@Qualifier("tossWebClient") WebClient tossWebClient) {
        this.tossWebClient = tossWebClient;
    }

    /** tossWebClient 빈 존재 여부 (toss.api.url 미설정 시 false). */
    public boolean isAvailable() {
        return tossWebClient != null;
    }

    /**
     * 토스 프로모션 리워드 지급 (2단계: Key 발급 → 리워드 실행)
     * @return 발급된 key
     */
    public String grantReward(String userKey, String promotionCode, int amount) {
        if (tossWebClient == null) {
            throw new TossPromotionException("토스 API가 설정되지 않았습니다. (toss.api.url 미설정)");
        }
        String key = getKey(userKey);
        executePromotion(userKey, promotionCode, key, amount);
        return key;
    }

    private String getKey(String userKey) {
        try {
            ApiResponse response = tossWebClient.post()
                    .uri(GET_KEY_PATH)
                    .header(USER_KEY_HEADER, userKey)
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .block();

            if (response == null || !"SUCCESS".equals(response.resultType) || response.success == null) {
                throw new TossPromotionException("Key 발급 실패: 응답 없음 또는 실패");
            }

            log.info("[TossPromotion] Key 발급 성공 (userKey={})", maskUserKey(userKey));
            return response.success.key;
        } catch (WebClientResponseException e) {
            log.error("[TossPromotion] Key 발급 HTTP 에러 (userKey={}, status={}, body={})",
                    maskUserKey(userKey), e.getStatusCode(), e.getResponseBodyAsString());
            throw new TossPromotionException("Key 발급 실패: HTTP " + e.getStatusCode(), e);
        }
    }

    private void executePromotion(String userKey, String promotionCode, String key, int amount) {
        try {
            ExecuteRequest request = new ExecuteRequest(promotionCode, key, amount);

            ApiResponse response = tossWebClient.post()
                    .uri(EXECUTE_PATH)
                    .header(USER_KEY_HEADER, userKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .block();

            if (response == null || !"SUCCESS".equals(response.resultType)) {
                String errorMsg = (response != null && response.error != null)
                        ? response.error.code + ": " + response.error.message
                        : "응답 없음";
                throw new TossPromotionException("리워드 지급 실패: " + errorMsg);
            }

            log.info("[TossPromotion] 리워드 지급 성공 (userKey={}, code={}, amount={})",
                    maskUserKey(userKey), promotionCode, amount);
        } catch (WebClientResponseException e) {
            log.error("[TossPromotion] 리워드 지급 HTTP 에러 (userKey={}, code={}, status={}, body={})",
                    maskUserKey(userKey), promotionCode, e.getStatusCode(), e.getResponseBodyAsString());
            throw new TossPromotionException("리워드 지급 실패: HTTP " + e.getStatusCode(), e);
        }
    }

    private String maskUserKey(String userKey) {
        if (userKey == null || userKey.length() < 8) return "***";
        return userKey.substring(0, 4) + "****" + userKey.substring(userKey.length() - 4);
    }

    // === 내부 DTO ===

    private record ExecuteRequest(String promotionCode, String key, int amount) {}

    private static class ApiResponse {
        public String resultType;
        public SuccessData success;
        public ErrorData error;
    }

    private static class SuccessData {
        public String key;
    }

    private static class ErrorData {
        public String code;
        public String message;
    }

    public static class TossPromotionException extends RuntimeException {
        public TossPromotionException(String message) {
            super(message);
        }
        public TossPromotionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
