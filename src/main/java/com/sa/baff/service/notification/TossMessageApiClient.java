package com.sa.baff.service.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * 토스 스마트 발송 메시지 API 클라이언트 (spec §3.1).
 *
 * 기능성 메시지 발송 (출석 리마인더 등).
 * - mTLS 인증: tossWebClient (WebClientConfig @ConditionalOnProperty name=toss.api.url)
 * - 사용자 식별: x-toss-user-key 헤더
 *   - 직접 토스 가입자: UserB.socialId (raw toss userKey)
 *   - 병합 Primary: AccountLink.providerUserId
 *
 * 체인지업 TossPromotionApiClient와 동일한 안전 주입 패턴 사용 (tossWebClient 미설정 환경에서도 로딩 허용).
 */
@Slf4j
@Component
public class TossMessageApiClient {

    private static final String SEND_MESSAGE_PATH = "/api-partner/v1/apps-in-toss/messenger/send-message";
    private static final String USER_KEY_HEADER = "x-toss-user-key";

    private WebClient tossWebClient;

    @Autowired(required = false)
    public void setTossWebClient(@Qualifier("tossWebClient") WebClient tossWebClient) {
        this.tossWebClient = tossWebClient;
    }

    /**
     * 기능성 메시지 발송.
     *
     * @param userKey          토스 사용자 식별 키 (직접 토스 유저면 socialId, 병합 유저면 account_links.provider_user_id)
     * @param templateSetCode  토스 콘솔 검수 완료된 템플릿 코드
     * @param context          템플릿 변수 (없으면 빈 Map)
     * @return 발송 상세 결과
     */
    public SendResult sendMessageWithDetail(String userKey, String templateSetCode, Map<String, String> context) {
        if (tossWebClient == null) {
            log.warn("[TossMessage] tossWebClient 미설정 (toss.api.url 없음). 발송 skip");
            return new SendResult(false, "NOT_CONFIGURED", null, "tossWebClient 미설정");
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "templateSetCode", templateSetCode,
                    "context", context != null ? context : Map.of()
            );

            SendResponse response = tossWebClient.post()
                    .uri(SEND_MESSAGE_PATH)
                    .header(USER_KEY_HEADER, userKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(SendResponse.class)
                    .block();

            if (response != null && "SUCCESS".equals(response.resultType)) {
                log.info("[TossMessage] 발송 성공 (userKey={}, template={})",
                        maskUserKey(userKey), templateSetCode);
                return new SendResult(true, "SUCCESS", null, null);
            }

            String errorCode = response != null && response.error != null ? response.error.errorCode : null;
            String errorReason = response != null && response.error != null ? response.error.reason : null;
            String resultType = response != null ? response.resultType : null;

            log.warn("[TossMessage] 발송 실패 (userKey={}, template={}, resultType={}, errorCode={}, reason={})",
                    maskUserKey(userKey), templateSetCode, resultType, errorCode, errorReason);
            return new SendResult(false, resultType, errorCode, errorReason);
        } catch (WebClientResponseException e) {
            log.error("[TossMessage] 발송 HTTP 에러 (userKey={}, template={}, status={}, body={})",
                    maskUserKey(userKey), templateSetCode, e.getStatusCode(), e.getResponseBodyAsString());
            return new SendResult(false, "HTTP_" + e.getStatusCode().value(), null, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[TossMessage] 발송 실패 (userKey={}, template={}): {}",
                    maskUserKey(userKey), templateSetCode, e.getMessage());
            return new SendResult(false, "EXCEPTION", null, e.getMessage());
        }
    }

    private String maskUserKey(String userKey) {
        if (userKey == null || userKey.length() < 8) return "***";
        return userKey.substring(0, 4) + "****" + userKey.substring(userKey.length() - 4);
    }

    public record SendResult(boolean success, String resultType, String errorCode, String errorReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SendResponse {
        public String resultType;
        public ErrorDetail error;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ErrorDetail {
        public String errorCode;
        public String reason;
        public String title;
    }
}
