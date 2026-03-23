package com.sa.baff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AdminDashboardDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminStats {
        private long totalUsers;
        private long newUsersThisWeek;
        private long activeBattles;
        private long totalReviews;
        private long totalInquiries;
        private long pendingInquiries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGrowth {
        private String label;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightTrend {
        private String label;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformDistribution {
        private String platform;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String type;
        private String message;
        private String timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserListItem {
        private Long userId;
        private String nickname;
        private String email;
        private String profileImageUrl;
        private String role;
        private String status;
        private String provider;
        private String platform;
        private Double height;
        private String regDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserDetail {
        private Long userId;
        private String nickname;
        private String email;
        private String profileImageUrl;
        private String role;
        private String status;
        private String provider;
        private String platform;
        private Double height;
        private String regDateTime;
        private long totalWeightRecords;
        private long totalGoals;
        private long totalBattles;
        private long totalReviews;
        private long pieceBalance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminInquiryListItem {
        private Long inquiryId;
        private Long userId;
        private String nickname;
        private String title;
        private String content;
        private String inquiryType;
        private String inquiryStatus;
        private String regDateTime;
        private int replyCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminInquiryDetail {
        private Long inquiryId;
        private Long userId;
        private String nickname;
        private String email;
        private String title;
        private String content;
        private String inquiryType;
        private String inquiryStatus;
        private String regDateTime;
        private List<InquiryReplyItem> replies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InquiryReplyItem {
        private Long replyId;
        private String content;
        private Long adminId;
        private String adminNickname;
        private String regDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminBattleListItem {
        private Long battleId;
        private String entryCode;
        private String name;
        private String hostNickname;
        private int participantCount;
        private int maxParticipants;
        private String status;
        private Integer durationDays;
        private Long betAmount;
        private String startDate;
        private String endDate;
        private String regDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminReviewListItem {
        private Long reviewId;
        private String title;
        private String authorNickname;
        private String difficulty;
        private String dietMethods;
        private boolean isPublic;
        private long likes;
        private long commentCount;
        private String regDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoticeItem {
        private Long id;
        private String title;
        private String content;
        private Boolean isActive;
        private String regDateTime;
        private String modDateTime;
    }
}
