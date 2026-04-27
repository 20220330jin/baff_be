package com.sa.baff.api;

import com.sa.baff.domain.AiFeatureConfig;
import com.sa.baff.domain.RewardConfig;
import com.sa.baff.domain.SmartPushConfig;
import com.sa.baff.domain.SmartPushHistory;
import com.sa.baff.model.dto.AdminDashboardDto;
import com.sa.baff.model.dto.AiAnalysisDto;
import com.sa.baff.repository.AiFeatureConfigRepository;
import com.sa.baff.repository.RewardConfigRepository;
import com.sa.baff.repository.SmartPushConfigRepository;
import com.sa.baff.repository.SmartPushHistoryRepository;
import com.sa.baff.service.AdminDashboardService;
import com.sa.baff.service.SmartPushService;
import com.sa.baff.util.AiFeatureType;
import com.sa.baff.util.RewardType;
import com.sa.baff.util.SmartPushTargetStrategy;
import com.sa.baff.util.SmartPushType;
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
    private final AiFeatureConfigRepository aiFeatureConfigRepository;
    private final SmartPushConfigRepository smartPushConfigRepository;
    private final SmartPushHistoryRepository smartPushHistoryRepository;
    private final SmartPushService smartPushService;

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
        Integer threshold = (Integer) body.get("threshold");
        String promotionCode = (String) body.get("promotionCode");

        RewardConfig config = new RewardConfig();
        config.setRewardType(RewardType.valueOf(rewardType));
        config.setAmount(amount != null ? amount : 1);
        config.setDailyLimit(dailyLimit);
        config.setDescription(description);
        config.setEnabled(enabled);
        config.setProbability(100);
        config.setIsFixed(true);
        config.setThreshold(threshold);
        if (promotionCode != null && !promotionCode.isBlank()) {
            config.setPromotionCode(promotionCode.trim());
        }

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
        if (body.containsKey("threshold")) config.setThreshold((Integer) body.get("threshold"));
        if (body.containsKey("promotionCode")) {
            String code = (String) body.get("promotionCode");
            config.setPromotionCode(code == null || code.isBlank() ? null : code.trim());
        }
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

    // ==================== AI 기능 관리 ====================

    @GetMapping("/ai/configs")
    public ResponseEntity<List<AiAnalysisDto.AiFeatureConfigResponse>> getAiFeatureConfigs() {
        List<AiFeatureConfig> configs = aiFeatureConfigRepository.findAll();

        // 설정이 없으면 기본값 생성
        if (configs.isEmpty()) {
            for (AiFeatureType type : AiFeatureType.values()) {
                AiFeatureConfig config = new AiFeatureConfig();
                config.setFeatureType(type);
                config.setEnabled(false);
                config.setDescription(type == AiFeatureType.RUNNING ? "달리기 AI 분석" : "간헐적 단식 AI 분석");
                aiFeatureConfigRepository.save(config);
                configs.add(config);
            }
        }

        List<AiAnalysisDto.AiFeatureConfigResponse> result = configs.stream()
                .map(c -> {
                    AiAnalysisDto.AiFeatureConfigResponse res = new AiAnalysisDto.AiFeatureConfigResponse();
                    res.setId(c.getId());
                    res.setFeatureType(c.getFeatureType());
                    res.setEnabled(c.getEnabled());
                    res.setDescription(c.getDescription());
                    return res;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/ai/configs/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> updateAiFeatureConfig(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        AiFeatureConfig config = aiFeatureConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AI 설정을 찾을 수 없습니다."));
        if (body.containsKey("enabled")) config.setEnabled((Boolean) body.get("enabled"));
        if (body.containsKey("description")) config.setDescription((String) body.get("description"));
        aiFeatureConfigRepository.save(config);
        return ResponseEntity.ok(Map.of("message", "AI 설정이 수정되었습니다."));
    }

    // ==================== 스마트발송 관리 ====================

    @GetMapping("/smart-push/configs")
    public List<SmartPushConfig> getSmartPushConfigs() {
        return smartPushConfigRepository.findAll();
    }

    @PutMapping("/smart-push/configs/{pushType}")
    @Transactional
    public ResponseEntity<Map<String, String>> updateSmartPushConfig(
            @PathVariable String pushType,
            @RequestBody Map<String, Object> body) {
        SmartPushType type = SmartPushType.valueOf(pushType);
        SmartPushConfig config = smartPushConfigRepository.findByPushType(type)
                .orElseGet(() -> {
                    SmartPushConfig newConfig = new SmartPushConfig();
                    newConfig.setPushType(type);
                    return newConfig;
                });

        if (body.containsKey("enabled")) config.setEnabled((Boolean) body.get("enabled"));
        if (body.containsKey("title")) config.setTitle((String) body.get("title"));
        if (body.containsKey("body")) config.setBody((String) body.get("body"));
        if (body.containsKey("deepLink")) config.setDeepLink((String) body.get("deepLink"));
        if (body.containsKey("thresholdDays")) config.setThresholdDays((Integer) body.get("thresholdDays"));
        if (body.containsKey("cronExpression")) config.setCronExpression((String) body.get("cronExpression"));
        if (body.containsKey("templateCode")) config.setTemplateCode((String) body.get("templateCode"));
        if (body.containsKey("targetStrategy")) {
            config.setTargetStrategy(SmartPushTargetStrategy.valueOf((String) body.get("targetStrategy")));
        }

        // 가드 3: enabled=true 전환 시 templateCode 필수 (spec §3.3)
        if (Boolean.TRUE.equals(config.getEnabled())
                && (config.getTemplateCode() == null || config.getTemplateCode().isBlank())) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("message", "템플릿 코드 없이 활성화할 수 없습니다."));
        }

        smartPushConfigRepository.save(config);
        return ResponseEntity.ok(Map.of("message", "스마트발송 설정이 수정되었습니다."));
    }

    @GetMapping("/smart-push/history")
    public Page<SmartPushHistory> getSmartPushHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("regDateTime").descending());
        return smartPushHistoryRepository.findAllByOrderByRegDateTimeDesc(pageable);
    }

    @PostMapping("/smart-push/trigger/{pushType}")
    public ResponseEntity<Map<String, String>> triggerSmartPush(@PathVariable String pushType) {
        SmartPushType type = SmartPushType.valueOf(pushType);

        // 가드 2: templateCode 없으면 수동 실행 차단 (spec §3.3)
        SmartPushConfig config = smartPushConfigRepository.findByPushType(type).orElse(null);
        if (config == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "스마트발송 설정이 없습니다."));
        }
        if (config.getTemplateCode() == null || config.getTemplateCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "템플릿 코드를 먼저 입력해주세요."));
        }

        smartPushService.executePush(type);
        return ResponseEntity.ok(Map.of("message", "스마트발송이 실행되었습니다."));
    }

    // ===== 그램경제 스냅샷 (S6-16) =====

    @GetMapping("/gram-economy/snapshot")
    public ResponseEntity<AdminDashboardDto.GramEconomySnapshot> getGramEconomySnapshot() {
        return ResponseEntity.ok(adminDashboardService.getGramEconomySnapshot());
    }
}
