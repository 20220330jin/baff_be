package com.sa.baff.api;

import com.sa.baff.domain.AdMetricDailyEntry;
import com.sa.baff.domain.AdMetricEntryRevisionLog;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.AdMetricAnalyticsDto;
import com.sa.baff.repository.AdMetricDailyEntryRepository;
import com.sa.baff.repository.AdMetricEntryRevisionLogRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.service.AdMetricAnalyticsService;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * P0 광고전략 — 일자별 토스 콘솔 reported 수치 입력/조회 API.
 *
 * Spring Security가 /api/admin/** 경로를 ADMIN 역할로 보호.
 *
 * 엔드포인트:
 *  - GET    /api/admin/ad-metrics/daily?date=YYYY-MM-DD            단건 조회
 *  - GET    /api/admin/ad-metrics/daily?from=...&to=...            범위 조회 (분석 탭 D-7)
 *  - POST   /api/admin/ad-metrics/daily                            신규 생성 (해당 일자 row 없을 때)
 *  - PUT    /api/admin/ad-metrics/daily/{date}                     D+7 미만 수정 (revision log 미적재)
 *  - PATCH  /api/admin/ad-metrics/daily/{date}                     D+7 이후 수정 (reason 필수, revision log 적재)
 *  - GET    /api/admin/ad-metrics/daily/{date}/revisions           특정 일자 변경 이력
 *
 * D+7 기준일: row.metricDate 기준. 즉 LocalDate.now() - metricDate >= 7d 이면 D+7 이후.
 */
@RestController
@RequestMapping("/api/admin/ad-metrics")
@RequiredArgsConstructor
public class AdMetricRestController {

    private static final String TABLE_NAME_DAILY = "AdMetricDailyEntry";
    private static final long D_PLUS_7_THRESHOLD_DAYS = 7;

    private final AdMetricDailyEntryRepository dailyRepository;
    private final AdMetricEntryRevisionLogRepository revisionLogRepository;
    private final UserRepository userRepository;
    private final AdMetricAnalyticsService analyticsService;

    // ==================== 조회 ====================

    @GetMapping("/daily")
    public ResponseEntity<?> get(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (date != null) {
            return ResponseEntity.ok(
                    dailyRepository.findByMetricDate(date).orElse(null)
            );
        }
        if (from != null && to != null) {
            List<AdMetricDailyEntry> rows =
                    dailyRepository.findByMetricDateBetweenOrderByMetricDateDesc(from, to);
            return ResponseEntity.ok(rows);
        }
        return ResponseEntity.badRequest().body(Map.of(
                "message", "date 또는 from/to 파라미터를 지정해주세요"
        ));
    }

    @GetMapping("/daily/{date}/revisions")
    public ResponseEntity<List<AdMetricEntryRevisionLog>> getRevisions(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(
                revisionLogRepository.findByTableNameAndRowMetricDateOrderByIdDesc(
                        TABLE_NAME_DAILY, date)
        );
    }

    // ==================== 신규 생성 ====================

    @PostMapping("/daily")
    @Transactional
    public ResponseEntity<?> create(
            @RequestBody AdMetricDailyRequest req,
            @AuthenticationPrincipal String adminSocialId) {

        if (req.getMetricDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "metricDate 필수"));
        }
        if (dailyRepository.findByMetricDate(req.getMetricDate()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "해당 일자 입력값이 이미 존재합니다. PUT 또는 PATCH로 수정해주세요"
            ));
        }

        Long actorAdminId = resolveAdminId(adminSocialId);

        AdMetricDailyEntry entry = new AdMetricDailyEntry();
        entry.setMetricDate(req.getMetricDate());
        applyRequest(entry, req);
        entry.setActorAdminId(actorAdminId);

        dailyRepository.save(entry);
        return ResponseEntity.ok(entry);
    }

    // ==================== D+7 미만 수정 ====================

    @PutMapping("/daily/{date}")
    @Transactional
    public ResponseEntity<?> update(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody AdMetricDailyRequest req,
            @AuthenticationPrincipal String adminSocialId) {

        AdMetricDailyEntry existing = dailyRepository.findByMetricDate(date).orElse(null);
        if (existing == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "입력값 없음. POST로 생성해주세요: " + date
            ));
        }

        if (isAfterDPlus7(date)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "D+7 이후 수정은 PATCH + reason 필수"
            ));
        }

        applyRequest(existing, req);
        existing.setActorAdminId(resolveAdminId(adminSocialId));
        dailyRepository.save(existing);
        return ResponseEntity.ok(existing);
    }

    // ==================== D+7 이후 수정 (revision log 적재) ====================

    @PatchMapping("/daily/{date}")
    @Transactional
    public ResponseEntity<?> patch(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody AdMetricDailyPatchRequest req,
            @AuthenticationPrincipal String adminSocialId) {

        AdMetricDailyEntry existing = dailyRepository.findByMetricDate(date).orElse(null);
        if (existing == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "입력값 없음: " + date
            ));
        }

        boolean afterDPlus7 = isAfterDPlus7(date);

        if (afterDPlus7 && (req.getReason() == null || req.getReason().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "D+7 이후 수정은 사유(reason) 필수"
            ));
        }

        Map<String, Map<String, Object>> changed = computeDiff(existing, req);
        applyRequest(existing, req);
        Long actorAdminId = resolveAdminId(adminSocialId);
        existing.setActorAdminId(actorAdminId);
        dailyRepository.save(existing);

        if (afterDPlus7 && !changed.isEmpty()) {
            AdMetricEntryRevisionLog log = new AdMetricEntryRevisionLog();
            log.setTableName(TABLE_NAME_DAILY);
            log.setRowMetricDate(date);
            log.setDiff(Map.of("changed", changed));
            log.setSchemaVersion(1);
            log.setReason(req.getReason());
            log.setActorAdminId(actorAdminId);
            revisionLogRepository.save(log);
        }

        return ResponseEntity.ok(existing);
    }

    // ==================== 분석 (집계) ====================

    /** 단일 일자 KPI 카드 (분석 탭 + AI 보고용 자동 채움) */
    @GetMapping("/kpi")
    public ResponseEntity<AdMetricAnalyticsDto.Kpi> getKpi(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(analyticsService.getKpi(date));
    }

    /** 일별 KPI 표 (분석 탭 D-7) */
    @GetMapping("/daily-table")
    public ResponseEntity<List<AdMetricAnalyticsDto.DailyTableRow>> getDailyTable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getDailyTable(from, to));
    }

    // ==================== 헬퍼 ====================

    private boolean isAfterDPlus7(LocalDate metricDate) {
        long days = ChronoUnit.DAYS.between(metricDate, LocalDate.now());
        return days >= D_PLUS_7_THRESHOLD_DAYS;
    }

    private Long resolveAdminId(String adminSocialId) {
        UserB admin = userRepository.findBySocialId(adminSocialId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다"));
        return admin.getId();
    }

    /**
     * request의 non-null 필드만 entry에 반영 (PATCH 시 부분 업데이트 허용).
     * POST/PUT에서도 동일하게 사용 — null인 필드는 건드리지 않음.
     * 명시적으로 null로 만들고 싶으면 별도 엔드포인트나 sentinel 값 도입 필요 (P0 범위 밖).
     */
    private void applyRequest(AdMetricDailyEntry entry, AdMetricDailyRequest req) {
        if (req.getTossRevenueR() != null) entry.setTossRevenueR(req.getTossRevenueR());
        if (req.getTossRevenueF() != null) entry.setTossRevenueF(req.getTossRevenueF());
        if (req.getTossRevenueBTotal() != null) entry.setTossRevenueBTotal(req.getTossRevenueBTotal());
        if (req.getTossRevenueI() != null) entry.setTossRevenueI(req.getTossRevenueI());

        if (req.getEcpmRReported() != null) entry.setEcpmRReported(req.getEcpmRReported());
        if (req.getEcpmFReported() != null) entry.setEcpmFReported(req.getEcpmFReported());
        if (req.getEcpmBTotalReported() != null) entry.setEcpmBTotalReported(req.getEcpmBTotalReported());
        if (req.getEcpmIReported() != null) entry.setEcpmIReported(req.getEcpmIReported());

        if (req.getImpressionRReported() != null) entry.setImpressionRReported(req.getImpressionRReported());
        if (req.getImpressionFReported() != null) entry.setImpressionFReported(req.getImpressionFReported());
        if (req.getImpressionBTotal() != null) entry.setImpressionBTotal(req.getImpressionBTotal());
        if (req.getImpressionI() != null) entry.setImpressionI(req.getImpressionI());

        if (req.getNewInflowToss() != null) entry.setNewInflowToss(req.getNewInflowToss());
        if (req.getAvgSessionSec() != null) entry.setAvgSessionSec(req.getAvgSessionSec());
        if (req.getBenefitsTabInflow() != null) entry.setBenefitsTabInflow(req.getBenefitsTabInflow());
    }

    /**
     * 변경된 컬럼만 추출 — {col: {"before": x, "after": y}}.
     * 요청에서 null인 필드는 변경 대상이 아님 (= 미입력).
     */
    private Map<String, Map<String, Object>> computeDiff(AdMetricDailyEntry before, AdMetricDailyRequest req) {
        Map<String, Map<String, Object>> changed = new LinkedHashMap<>();
        addIfChanged(changed, "tossRevenueR", before.getTossRevenueR(), req.getTossRevenueR());
        addIfChanged(changed, "tossRevenueF", before.getTossRevenueF(), req.getTossRevenueF());
        addIfChanged(changed, "tossRevenueBTotal", before.getTossRevenueBTotal(), req.getTossRevenueBTotal());
        addIfChanged(changed, "tossRevenueI", before.getTossRevenueI(), req.getTossRevenueI());

        addIfChanged(changed, "ecpmRReported", before.getEcpmRReported(), req.getEcpmRReported());
        addIfChanged(changed, "ecpmFReported", before.getEcpmFReported(), req.getEcpmFReported());
        addIfChanged(changed, "ecpmBTotalReported", before.getEcpmBTotalReported(), req.getEcpmBTotalReported());
        addIfChanged(changed, "ecpmIReported", before.getEcpmIReported(), req.getEcpmIReported());

        addIfChanged(changed, "impressionRReported", before.getImpressionRReported(), req.getImpressionRReported());
        addIfChanged(changed, "impressionFReported", before.getImpressionFReported(), req.getImpressionFReported());
        addIfChanged(changed, "impressionBTotal", before.getImpressionBTotal(), req.getImpressionBTotal());
        addIfChanged(changed, "impressionI", before.getImpressionI(), req.getImpressionI());

        addIfChanged(changed, "newInflowToss", before.getNewInflowToss(), req.getNewInflowToss());
        addIfChanged(changed, "avgSessionSec", before.getAvgSessionSec(), req.getAvgSessionSec());
        addIfChanged(changed, "benefitsTabInflow", before.getBenefitsTabInflow(), req.getBenefitsTabInflow());
        return changed;
    }

    private static void addIfChanged(Map<String, Map<String, Object>> changed,
                                     String key, Object before, Object after) {
        if (after == null) return;
        if (Objects.equals(before, after)) return;
        Map<String, Object> entry = new HashMap<>();
        entry.put("before", before);
        entry.put("after", after);
        changed.put(key, entry);
    }

    // ==================== Request DTOs ====================

    @Getter
    @Setter
    public static class AdMetricDailyRequest {
        private LocalDate metricDate;

        private Integer tossRevenueR;
        private Integer tossRevenueF;
        private Integer tossRevenueBTotal;
        private Integer tossRevenueI;

        private Integer ecpmRReported;
        private Integer ecpmFReported;
        private Integer ecpmBTotalReported;
        private Integer ecpmIReported;

        private Integer impressionRReported;
        private Integer impressionFReported;
        private Integer impressionBTotal;
        private Integer impressionI;

        private Integer newInflowToss;
        private BigDecimal avgSessionSec;
        private Integer benefitsTabInflow;
    }

    @Getter
    @Setter
    public static class AdMetricDailyPatchRequest extends AdMetricDailyRequest {
        /** D+7 이후 수정 시 NOT NULL */
        private String reason;
    }
}
