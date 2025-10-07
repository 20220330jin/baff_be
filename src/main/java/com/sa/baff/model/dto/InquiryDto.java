package com.sa.baff.model.dto;

import com.sa.baff.domain.type.InquiryStatus;
import com.sa.baff.domain.type.InquiryType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class InquiryDto {

    @Setter
    @Getter
    public static class getInquiryList {
        private Long inquiryId;
        private String title;
        private String content;
        private InquiryType inquiryType;
        private InquiryStatus inquiryStatus;
        private LocalDateTime regDateTime;

        public getInquiryList(Long inquiryId, String title, String content, InquiryType inquiryType, InquiryStatus inquiryStatus, LocalDateTime regDateTime) {
            this.inquiryId = inquiryId;
            this.title = title;
            this.content = content;
            this.inquiryType = inquiryType;
            this.inquiryStatus = inquiryStatus;
            this.regDateTime = regDateTime;
        }
    }
}
