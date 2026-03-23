package com.sa.baff.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@ConditionalOnProperty(name = "changeup.reward.enabled", havingValue = "true")
public class TossPromotionApiClient {

    private static final String GET_KEY_PATH = "/api-public/v1/promotion/get-key";
    private static final String EXECUTE_PATH = "/api-public/v1/promotion/execute";
    private static final String USER_KEY_HEADER = "x-toss-user-key";

    private final WebClient tossWebClient;

    @Autowired
    public TossPromotionApiClient(@Qualifier("tossWebClient") WebClient tossWebClient) {
        this.tossWebClient = tossWebClient;
    }

    /**
     * 토스 프로모션 리워드 지급 (2단계: Key 발급 → 리워드 실행)
     * @return 발급된 key
     */
    public String grantReward(String userKey, String promotionCode, int amount) {
        String key = getKey(userKey);
        executePromotion(userKey, promotionCode, key, amount);
        return key;
    }

    private String getKey(String userKey) {
        ApiResponse response = tossWebClient.post()
                .uri(GET_KEY_PATH)
                .header(USER_KEY_HEADER, userKey)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .block();

        if (response == null || !"SUCCESS".equals(response.resultType) || response.success == null) {
            String errorMsg = response != null && response.error != null
                    ? response.error.code + ": " + response.error.message
                    : "응답 없음";
            throw new TossPromotionException("Key 발급 실패: " + errorMsg);
        }

        return response.success.key;
    }

    private void executePromotion(String userKey, String promotionCode, String key, int amount) {
        ExecuteRequest request = new ExecuteRequest(promotionCode, key, amount);

        ApiResponse response = tossWebClient.post()
                .uri(EXECUTE_PATH)
                .header(USER_KEY_HEADER, userKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .block();

        if (response == null || !"SUCCESS".equals(response.resultType)) {
            String errorMsg = response != null && response.error != null
                    ? response.error.code + ": " + response.error.message
                    : "응답 없음";
            throw new TossPromotionException("리워드 지급 실패: " + errorMsg);
        }

        log.info("토스 프로모션 실행 성공: promotionCode={}, amount={}", promotionCode, amount);
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
    }
}
