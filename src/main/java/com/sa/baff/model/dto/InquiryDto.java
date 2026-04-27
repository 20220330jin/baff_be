package com.sa.baff.model.dto;

import com.sa.baff.domain.type.InquiryStatus;
import com.sa.baff.domain.type.InquiryType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        private List<InquiryReplyDto> replies = new ArrayList<>();

        public getInquiryList(Long inquiryId, String title, String content, InquiryType inquiryType, InquiryStatus inquiryStatus, LocalDateTime regDateTime) {
            this.inquiryId = inquiryId;
            this.title = title;
            this.content = content;
            this.inquiryType = inquiryType;
            this.inquiryStatus = inquiryStatus;
            this.regDateTime = regDateTime;
        }
    }

    @Setter
    @Getter
    public static class InquiryReplyDto {
        private Long replyId;
        private Long inquiryId;
        private String content;
        private Long adminId;
        private LocalDateTime regDateTime;

        public InquiryReplyDto(Long replyId, Long inquiryId, String content, Long adminId, LocalDateTime regDateTime) {
            this.replyId = replyId;
            this.inquiryId = inquiryId;
            this.content = content;
            this.adminId = adminId;
            this.regDateTime = regDateTime;
        }
    }

    @Setter
    @Getter
    public static class getAdminInquiryList {
        private Long inquiryId;
        private Long userId;
        private String nickname;
        private String title;
        private String content;
        private InquiryType inquiryType;
        private InquiryStatus inquiryStatus;
        private LocalDateTime regDateTime;

        public getAdminInquiryList(Long inquiryId, Long userId, String nickname, String title, String content, InquiryType inquiryType, InquiryStatus inquiryStatus, LocalDateTime regDateTime) {
            this.inquiryId = inquiryId;
            this.userId = userId;
            this.nickname = nickname;
            this.title = title;
            this.content = content;
            this.inquiryType = inquiryType;
            this.inquiryStatus = inquiryStatus;
            this.regDateTime = regDateTime;
        }
    }
}
