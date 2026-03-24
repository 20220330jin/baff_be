package com.sa.baff.service;

import com.sa.baff.model.dto.AdminDashboardDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminDashboardService {

    // === 대시보드 개요 ===
    AdminDashboardDto.AdminStats getAdminStats();
    List<AdminDashboardDto.UserGrowth> getUserGrowth(String period);
    List<AdminDashboardDto.WeightTrend> getWeightTrend(String period);
    List<AdminDashboardDto.PlatformDistribution> getPlatformDistribution();
    List<AdminDashboardDto.RecentActivity> getRecentActivities();

    // === 사용자 관리 ===
    Page<AdminDashboardDto.AdminUserListItem> getUsers(String status, String search, Pageable pageable);
    AdminDashboardDto.AdminUserDetail getUserDetail(Long userId);
    void updateUserRole(Long userId, String role);
    void updateUserStatus(Long userId, String status);

    // === 문의 관리 ===
    Page<AdminDashboardDto.AdminInquiryListItem> getInquiries(String status, String type, String search, Pageable pageable);
    AdminDashboardDto.AdminInquiryDetail getInquiryDetail(Long inquiryId);
    void replyToInquiry(Long inquiryId, String content, String adminSocialId);
    void updateInquiryStatus(Long inquiryId, String status);

    // === 배틀 관리 ===
    Page<AdminDashboardDto.AdminBattleListItem> getBattles(String status, Pageable pageable);

    // === 리뷰 관리 ===
    Page<AdminDashboardDto.AdminReviewListItem> getReviews(String search, Pageable pageable);
    void updateReviewVisibility(Long reviewId, boolean isPublic);
    void deleteReview(Long reviewId);

    // === 공지사항 ===
    List<AdminDashboardDto.NoticeItem> getNotices();
    AdminDashboardDto.NoticeItem createNotice(String title, String content);
    AdminDashboardDto.NoticeItem updateNotice(Long noticeId, String title, String content, Boolean isActive);
    void deleteNotice(Long noticeId);

    // === 리워드 관리 ===
    AdminDashboardDto.AdminRewardSummary getRewardSummary();
    Page<AdminDashboardDto.AdminRewardConfigItem> getRewardConfigs(Pageable pageable);
    Page<AdminDashboardDto.AdminExchangeItem> getRewardExchanges(Pageable pageable);

    // === 내역 관리 ===
    Page<AdminDashboardDto.LoginHistoryItem> getLoginHistories(Pageable pageable);
    Page<AdminDashboardDto.WeightHistoryItem> getWeightHistories(Pageable pageable);

    // === 광고 시청 관리 ===
    AdminDashboardDto.AdWatchSummary getAdWatchSummary();
    Page<AdminDashboardDto.AdWatchHistoryItem> getAdWatchHistory(Pageable pageable);

    // === 토스광고 설정 ===
    List<AdminDashboardDto.TossAdPositionConfig> getTossAdConfigs();
    void updateTossAdConfig(String position, AdminDashboardDto.UpdateTossAdConfigRequest request);
}
