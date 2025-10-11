package com.sa.baff.service;

import com.sa.baff.common.AdminSyncStatus;
import com.sa.baff.domain.Inquiry;
import com.sa.baff.domain.type.InquiryStatus;
import com.sa.baff.domain.type.InquiryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AdminApiClient {

    // 비동기 http 통신
    private final WebClient webClient;

    // url 주입 -> application.yml
    private final String adminUrl;

    public AdminApiClient(WebClient webClient, @Value("${admin.url}") String adminUrl) {
        this.webClient = webClient;
        this.adminUrl = adminUrl;
    }

    public AdminSyncStatus sendInquiry(Inquiry inquiry) {
        try {
            System.out.println("Sending inquiry to admin server... inquiry Id: " + inquiry.getId());

            AdminInquiryRequest requestDto = new AdminInquiryRequest(inquiry);
            log.info("Sending inquiry to admin server... URL: {}, Inquiry Id: {}", adminUrl, inquiry.getId());

            AdminInquiryResponse response = webClient.post()
                    .uri(adminUrl + "/api/v1/inquiry")
                    .header("X-Source-Service", "CHANGEUP")
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(AdminInquiryResponse.class)
                    .block();
            // 통합어드민 전달 성공여부
            if (response != null && "SUCCESS".equalsIgnoreCase(response.status)) {

                log.info("Successfully sent inquiry to admin server. Report ID: {}", inquiry.getId());
                return AdminSyncStatus.SUCCESS;
            } else {
                log.warn("Failed to send inquiry to admin server. Report ID: {}", inquiry.getId());
                return AdminSyncStatus.FAILED;
            }
        } catch (Exception e) {
            log.error("Error sending inquiry to admin server. Report ID: {}: {}", inquiry.getId(), e.getMessage());
            return AdminSyncStatus.FAILED;
        }
    }

    /**
     * 통합 어드민 데이터 요청
     */
    private static class AdminInquiryRequest {
        public Long inquiryId;
        public String title;
        public String content;
        public InquiryType inquiryType;
        public InquiryStatus status;
        public LocalDateTime regDateTime;
        public Long userId;

        public AdminInquiryRequest(Inquiry inquiry) {
            this.inquiryId = inquiry.getId();
            this.title = inquiry.getTitle();
            this.content = inquiry.getContent();
            this.inquiryType = inquiry.getInquiryType();
            this.status = inquiry.getStatus();
            this.regDateTime = inquiry.getRegDateTime();
            this.userId = inquiry.getUser().getId();
        }
    }

    /**
     * 통합 어드민 반환 DTO 용도(내부용도)
     */
    private static class AdminInquiryResponse {
        public String status;
        public String message;
        public LocalDateTime processedAt;
    }

}
