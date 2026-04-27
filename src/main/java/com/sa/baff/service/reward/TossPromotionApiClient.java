package com.sa.baff.service.reward;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 토스 프로모션 API 클라이언트 (비게임, 서버-to-서버)
 *
 * 흐름: Key 발급 → 리워드 지급 → (선택) 결과 조회
 * - mTLS 인증: tossWebClient (WebClientConfig에서 설정, toss.api.url 미설정 시 빈 미생성)
 * - 사용자 식별: x-toss-user-key 헤더 (= socialId)
 *
 * 나만그래(onlyme) TossPromotionApiClient 패턴 복제. 토스 콘솔 승인/프로모션 코드 발급 후 활성화.
 */
@Slf4j
@Component
public class TossPromotionApiClient {

    private static final String GET_KEY_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion/get-key";
    private static final String EXECUTE_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion";
    private static final String RESULT_PATH = "/api-partner/v1/apps-in-toss/promotion/execution-result";
    private static final String USER_KEY_HEADER = "x-toss-user-key";

    private final WebClient tossWebClient;

    public TossPromotionApiClient(@Autowired(required = false) @Qualifier("tossWebClient") WebClient tossWebClient) {
        this.tossWebClient = tossWebClient;
    }

    /** tossWebClient 빈이 없으면 false (toss.api.url 미설정). 호출 전 가드용. */
    public boolean isAvailable() {
        return tossWebClient != null;
    }

    /**
     * 프로모션 리워드 지급 (Key 발급 → 지급 실행을 한 번에 수행)
     *
     * @param userKey       토스 사용자 식별 키 (= socialId)
     * @param promotionCode 콘솔에서 발급받은 프로모션 코드
     * @param amount        지급 금액 (토스포인트)
     * @return 지급에 사용된 key (결과 조회용)
     * @throws TossPromotionException 지급 실패 시
     */
    public String grantReward(String userKey, String promotionCode, int amount) {
        if (tossWebClient == null) {
            throw new TossPromotionException("tossWebClient 미설정 (toss.api.url 누락)");
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
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .block();

            if (response == null || !"SUCCESS".equals(response.resultType) || response.success == null) {
                String errorMsg = formatError(response);
                log.warn("[TossPromotion] Key 발급 실패 (userKey={}, resultType={}, error={})",
                        maskUserKey(userKey), response != null ? response.resultType : null, errorMsg);
                throw new TossPromotionException("Key 발급 실패: " + errorMsg);
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .block();

            if (response == null || !"SUCCESS".equals(response.resultType)) {
                String errorMsg = formatError(response);
                log.warn("[TossPromotion] 리워드 지급 실패 (userKey={}, code={}, amount={}, resultType={}, error={})",
                        maskUserKey(userKey), promotionCode, amount, response != null ? response.resultType : null, errorMsg);
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

    private String formatError(ApiResponse response) {
        if (response == null) return "응답 없음";
        if (response.error == null) return response.resultType != null ? response.resultType : "응답 없음";

        String code = firstNonBlank(response.error.errorCode, response.error.code);
        String message = firstNonBlank(response.error.message, response.error.reason, response.error.title);
        if (code != null && message != null) return code + ": " + message;
        if (code != null) return code;
        if (message != null) return message;
        return response.resultType != null ? response.resultType : "응답 없음";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private record ExecuteRequest(String promotionCode, String key, int amount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ApiResponse {
        public String resultType;
        public SuccessData success;
        public ErrorData error;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SuccessData {
        public String key;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ErrorData {
        public String errorCode;
        public String code;
        public String message;
        public String reason;
        public String title;
    }

    public static class TossPromotionException extends RuntimeException {
        public TossPromotionException(String message) { super(message); }
        public TossPromotionException(String message, Throwable cause) { super(message, cause); }
    }
}
