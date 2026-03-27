package com.sa.baff.api;

import com.sa.baff.domain.RewardConfig;
import com.sa.baff.model.dto.AdminDashboardDto;
import com.sa.baff.repository.RewardConfigRepository;
import com.sa.baff.service.AdminDashboardService;
import com.sa.baff.util.RewardType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 대시보드 관련 APIs
 * Spring Security가 /api/admin/** 경로를 ADMIN 역할만 접근하도록 보호
 *
 * @author hjkim
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardRestController {

    private final AdminDashboardService adminDashboardService;
    private final RewardConfigRepository rewardConfigRepository;

    // ==================== 대시보드 개요 ====================

    /**
     * 관리자 통계 조회
     * @return 관리자 통계 정보 (총 사용자, 활성 배틀, 리뷰 수 등)
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardDto.AdminStats> getAdminStats() {
        return ResponseEntity.ok(adminDashboardService.getAdminStats());
    }

    /**
     * 기간별 사용자 증가 추이 조회
     * @param period 조회 기간 타입 (DAILY, WEEKLY, MONTHLY)
     * @return 기간별 사용자 증가 데이터
     */
    @GetMapping("/user-growth")
    public ResponseEntity<List<AdminDashboardDto.UserGrowth>> getUserGrowth(
            @RequestParam(defaultValue = "DAILY") String period) {
        return ResponseEntity.ok(adminDashboardService.getUserGrowth(period));
    }

    /**
     * 기간별 체중 기록 트렌드 조회
     * @param period 조회 기간 타입 (DAILY, WEEKLY, MONTHLY)
     * @return 기간별 체중 기록 트렌드 데이터
     */
    @GetMapping("/weight-trend")
    public ResponseEntity<List<AdminDashboardDto.WeightTrend>> getWeightTrend(
            @RequestParam(defaultValue = "DAILY") String period) {
        return ResponseEntity.ok(adminDashboardService.getWeightTrend(period));
    }

    /**
     * 플랫폼별 사용자 분포 조회
     * @return 플랫폼별 분포 데이터
     */
    @GetMapping("/platform-distribution")
    public ResponseEntity<List<AdminDashboardDto.PlatformDistribution>> getPlatformDistribution() {
        return ResponseEntity.ok(adminDashboardService.getPlatformDistribution());
    }

    /**
     * 최근 활동 내역 조회
     * @return 최근 활동 목록
     */
    @GetMapping("/recent-activities")
    public ResponseEntity<List<AdminDashboardDto.RecentActivity>> getRecentActivities() {
        return ResponseEntity.ok(adminDashboardService.getRecentActivities());
    }

    // ==================== 사용자 관리 ====================

    /**
     * 사용자 목록 조회
     * @param status 사용자 상태 필터 (ACTIVE, INACTIVE 등)
     * @param search 검색 키워드 (닉네임, 이메일)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 사용자 목록 페이지 응답
     */
    @GetMapping("/users")
    public ResponseEntity<Page<AdminDashboardDto.AdminUserListItem>> getUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminDashboardService.getUsers(status, search, pageable));
    }

    /**
     * 사용자 상세 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 상세 정보
     */
    @GetMapping("/users/{userId}/detail")
    public ResponseEntity<AdminDashboardDto.AdminUserDetail> getUserDetail(
            @PathVariable Long userId) {
        return ResponseEntity.ok(adminDashboardService.getUserDetail(userId));
    }

    /**
     * 사용자 역할 변경
     * @param userId 사용자 ID
     * @param body role 필드 포함
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        String role = (String) body.get("role");
        adminDashboardService.updateUserRole(userId, role);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자 상태 변경
     * @param userId 사용자 ID
     * @param body status 필드 포함
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        adminDashboardService.updateUserStatus(userId, status);
        return ResponseEntity.ok().build();
    }

    // ==================== 문의 관리 ====================

    /**
     * 문의 목록 조회
     * @param status 문의 상태 필터
     * @param type 문의 유형 필터
     * @param search 검색 키워드
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 문의 목록 페이지 응답
     */
    @GetMapping("/inquiries")
    public ResponseEntity<Page<AdminDashboardDto.AdminInquiryListItem>> getInquiries(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminDashboardService.getInquiries(status, type, search, pageable));
    }

    /**
     * 문의 상세 조회
     * @param id 문의 ID
     * @return 문의 상세 정보
     */
    @GetMapping("/inquiries/{id}")
    public ResponseEntity<AdminDashboardDto.AdminInquiryDetail> getInquiryDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminDashboardService.getInquiryDetail(id));
    }

    /**
     * 문의 답변 등록
     * @param id 문의 ID
     * @param body content 필드 포함
     * @param adminSocialId 관리자 socialId (JWT에서 추출)
     */
    @PostMapping("/inquiries/{id}/reply")
    public ResponseEntity<Void> replyToInquiry(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal String adminSocialId) {
        String content = (String) body.get("content");
        adminDashboardService.replyToInquiry(id, content, adminSocialId);
        return ResponseEntity.ok().build();
    }

    /**
     * 문의 상태 변경
     * @param id 문의 ID
     * @param body status 필드 포함
     */
    @PutMapping("/inquiries/{id}/status")
    public ResponseEntity<Void> updateInquiryStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        adminDashboardService.updateInquiryStatus(id, status);
        return ResponseEntity.ok().build();
    }

    // ==================== 배틀 관리 ====================

    /**
     * 배틀 목록 조회
     * @param status 배틀 상태 필터
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 배틀 목록 페이지 응답
     */
    @GetMapping("/battles")
    public ResponseEntity<Page<AdminDashboardDto.AdminBattleListItem>> getBattles(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminDashboardService.getBattles(status, pageable));
    }

    // ==================== 리뷰 관리 ====================

    /**
     * 리뷰 목록 조회
     * @param search 검색 키워드
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 리뷰 목록 페이지 응답
     */
    @GetMapping("/reviews")
    public ResponseEntity<Page<AdminDashboardDto.AdminReviewListItem>> getReviews(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminDashboardService.getReviews(search, pageable));
    }

    /**
     * 리뷰 공개/비공개 변경
     * @param id 리뷰 ID
     * @param body isPublic 필드 포함
     */
    @PutMapping("/reviews/{id}/visibility")
    public ResponseEntity<Void> updateReviewVisibility(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        boolean isPublic = (boolean) body.get("isPublic");
        adminDashboardService.updateReviewVisibility(id, isPublic);
        return ResponseEntity.ok().build();
    }

    /**
     * 리뷰 삭제
     * @param id 리뷰 ID
     */
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        adminDashboardService.deleteReview(id);
        return ResponseEntity.ok().build();
    }

    // ==================== 공지사항 ====================

    /**
     * 공지사항 목록 조회
     * @return 공지사항 목록
     */
    @GetMapping("/notices")
    public ResponseEntity<List<AdminDashboardDto.NoticeItem>> getNotices() {
        return ResponseEntity.ok(adminDashboardService.getNotices());
    }

    /**
     * 공지사항 생성
     * @param body title, content 필드 포함
     * @return 생성된 공지사항
     */
    @PostMapping("/notices")
    public ResponseEntity<AdminDashboardDto.NoticeItem> createNotice(
            @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        return ResponseEntity.ok(adminDashboardService.createNotice(title, content));
    }

    /**
     * 공지사항 수정
     * @param id 공지사항 ID
     * @param body title, content, isActive 필드 포함
     * @return 수정된 공지사항
     */
    @PutMapping("/notices/{id}")
    public ResponseEntity<AdminDashboardDto.NoticeItem> updateNotice(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Boolean isActive = (Boolean) body.get("isActive");
        return ResponseEntity.ok(adminDashboardService.updateNotice(id, title, content, isActive));
    }

    /**
     * 공지사항 삭제
     * @param id 공지사항 ID
     */
    @DeleteMapping("/notices/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        adminDashboardService.deleteNotice(id);
        return ResponseEntity.ok().build();
    }

    // ==================== 리워드/조각 관리 ====================

    @GetMapping("/rewards/summary")
    public ResponseEntity<AdminDashboardDto.AdminRewardSummary> getRewardSummary() {
        return ResponseEntity.ok(adminDashboardService.getRewardSummary());
    }

    @GetMapping("/rewards/configs")
    public ResponseEntity<Page<AdminDashboardDto.AdminRewardConfigItem>> getRewardConfigs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminDashboardService.getRewardConfigs(PageRequest.of(page, size)));
    }

    @PostMapping("/rewards/configs")
    public ResponseEntity<Map<String, Object>> createRewardConfig(@RequestBody Map<String, Object> body) {
        String rewardType = (String) body.get("rewardType");
        Integer amount = (Integer) body.get("amount");
        Integer dailyLimit = (Integer) body.get("dailyLimit");
        String description = (String) body.get("description");
        Boolean enabled = body.get("enabled") != null ? (Boolean) body.get("enabled") : true;

        RewardConfig config = new RewardConfig();
        config.setRewardType(RewardType.valueOf(rewardType));
        config.setAmount(amount != null ? amount : 1);
        config.setDailyLimit(dailyLimit);
        config.setDescription(description);
        config.setEnabled(enabled);
        config.setProbability(100);
        config.setIsFixed(true);

        rewardConfigRepository.save(config);
        return ResponseEntity.ok(Map.of("id", config.getId(), "message", "설정이 추가되었습니다."));
    }

    @PutMapping("/rewards/configs/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> updateRewardConfig(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        RewardConfig config = rewardConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("설정을 찾을 수 없습니다."));
        if (body.containsKey("amount")) config.setAmount((Integer) body.get("amount"));
        if (body.containsKey("dailyLimit")) config.setDailyLimit((Integer) body.get("dailyLimit"));
        if (body.containsKey("description")) config.setDescription((String) body.get("description"));
        if (body.containsKey("enabled")) config.setEnabled((Boolean) body.get("enabled"));
        rewardConfigRepository.save(config);
        return ResponseEntity.ok(Map.of("message", "설정이 수정되었습니다."));
    }

    @GetMapping("/rewards/exchanges")
    public ResponseEntity<Page<AdminDashboardDto.AdminExchangeItem>> getRewardExchanges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminDashboardService.getRewardExchanges(PageRequest.of(page, size)));
    }

    // ==================== 내역 관리 ====================

    @GetMapping("/history/logins")
    public ResponseEntity<Page<AdminDashboardDto.LoginHistoryItem>> getLoginHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminDashboardService.getLoginHistories(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "regDateTime"))));
    }

    @GetMapping("/history/weights")
    public ResponseEntity<Page<AdminDashboardDto.WeightHistoryItem>> getWeightHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminDashboardService.getWeightHistories(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordDate"))));
    }

    // ==================== 광고 시청 관리 ====================

    @GetMapping("/ad-watch/summary")
    public ResponseEntity<AdminDashboardDto.AdWatchSummary> getAdWatchSummary() {
        return ResponseEntity.ok(adminDashboardService.getAdWatchSummary());
    }

    @GetMapping("/ad-watch/history")
    public ResponseEntity<Page<AdminDashboardDto.AdWatchHistoryItem>> getAdWatchHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminDashboardService.getAdWatchHistory(PageRequest.of(page, size)));
    }

    // ==================== 토스광고 설정 ====================

    @GetMapping("/toss-ad/configs")
    public ResponseEntity<List<AdminDashboardDto.TossAdPositionConfig>> getTossAdConfigs() {
        return ResponseEntity.ok(adminDashboardService.getTossAdConfigs());
    }

    @PutMapping("/toss-ad/configs/{position}")
    public ResponseEntity<Void> updateTossAdConfig(
            @PathVariable String position,
            @RequestBody AdminDashboardDto.UpdateTossAdConfigRequest request) {
        adminDashboardService.updateTossAdConfig(position, request);
        return ResponseEntity.ok().build();
    }
}
