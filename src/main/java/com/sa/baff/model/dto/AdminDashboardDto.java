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

    // ===== 리워드 관리 =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminRewardSummary {
        private long totalIssuedPieces;
        private long totalBurnedPieces;
        private long currentCirculating;
        private long totalExchangeAmount;
        private long todayIssuedPieces;
        private long todayBurnedPieces;
        private long activeRewardUsers;
        private long pendingExchanges;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminRewardConfigItem {
        private Long configId;
        private String rewardType;
        private String actionType;
        private long pieceAmount;
        private String description;
        private boolean isActive;
        private String regDateTime;
        private String modDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminExchangeItem {
        private Long exchangeId;
        private Long userId;
        private String nickname;
        private long pieceAmount;
        private long exchangeAmount;
        private String status;
        private String regDateTime;
    }

    // ===== 내역 관리 =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginHistoryItem {
        private Long id;
        private Long userId;
        private String nickname;
        private String userAgent;
        private String loginDateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightHistoryItem {
        private Long id;
        private Long userId;
        private String nickname;
        private Double weight;
        private String recordDate;
        private String regDateTime;
    }

    // ===== 광고 시청 관리 =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdWatchSummary {
        private long totalWatchCount;
        private long todayWatchCount;
        private long uniqueUsers;
        private long todayUniqueUsers;
        private List<AdWatchLocationStat> locationStats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdWatchLocationStat {
        private String location;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdWatchHistoryItem {
        private Long id;
        private Long userId;
        private String nickname;
        private String watchLocation;
        private Long referenceId;
        private String tossAdResponse;
        private String regDateTime;
    }

    // ===== 토스광고 설정 =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TossAdPositionConfig {
        private Long id;
        private String position;
        private Integer tossAdRatio;
        private String tossAdGroupId;
        private Boolean isTossAdEnabled;
        private String tossImageAdGroupId;
        private Integer tossImageAdRatio;
        private Boolean isTossImageAdEnabled;
        private String tossBannerAdGroupId;
        private Integer tossBannerAdRatio;
        private Boolean isTossBannerAdEnabled;
        private String tossInterstitialAdGroupId;
        private Boolean isTossInterstitialAdEnabled;
        private Integer rewardedAdRatio;
        private Integer rewardedAdGrams;
        private Integer interstitialAdGrams;
        private String regDateTime;
        private String modDateTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTossAdConfigRequest {
        private Integer tossAdRatio;
        private String tossAdGroupId;
        private Boolean isTossAdEnabled;
        private String tossImageAdGroupId;
        private Integer tossImageAdRatio;
        private Boolean isTossImageAdEnabled;
        private String tossBannerAdGroupId;
        private Integer tossBannerAdRatio;
        private Boolean isTossBannerAdEnabled;
        private String tossInterstitialAdGroupId;
        private Boolean isTossInterstitialAdEnabled;
        private Integer rewardedAdRatio;
        private Integer rewardedAdGrams;
        private Integer interstitialAdGrams;
    }

    /**
     * S6-16 — 그램경제 스냅샷 (나만그래 pieceEconomyResponse 참조, 체인지업 MVP 단순화).
     *
     *  발행: totalEarned(누적) + todayIssued(오늘 REWARD_*)
     *  유통: circulating(현재 잔액 합) + holdersCount + avgBalance
     *  전환: totalExchanged(누적) + todayExchanged(오늘 EXCHANGE_REQUEST) + exchangeRate(전환율 %)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GramEconomySnapshot {
        // 발행
        private long totalEarned;
        private long todayIssued;

        // 유통
        private long circulating;
        private long holdersCount;
        private long avgBalance;

        // 전환
        private long totalExchanged;
        private long todayExchanged;
        private double exchangeRate;   // totalExchanged / totalEarned * 100
    }
}
