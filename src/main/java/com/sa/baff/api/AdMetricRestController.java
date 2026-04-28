package com.sa.baff.api;

import com.sa.baff.domain.AdMetricBannerEntry;
import com.sa.baff.domain.AdMetricDailyEntry;
import com.sa.baff.domain.AdMetricDeployMarker;
import com.sa.baff.domain.AdMetricEntryRevisionLog;
import com.sa.baff.domain.AdMetricImageEntry;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.AdMetricAnalyticsDto;
import com.sa.baff.repository.AdMetricBannerEntryRepository;
import com.sa.baff.repository.AdMetricDailyEntryRepository;
import com.sa.baff.repository.AdMetricDeployMarkerRepository;
import com.sa.baff.repository.AdMetricEntryRevisionLogRepository;
import com.sa.baff.repository.AdMetricImageEntryRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.service.AdMetricAnalyticsService;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 광고전략 — 일자별 토스 콘솔 reported 수치 + 위치별 분해 입력/조회 API.
 *
 * Spring Security가 /api/admin/** 경로를 ADMIN 역할로 보호.
 * 나만그래 패턴 정합: R / F 일별 + B 위치별 + I 위치별 분리 입력.
 */
@RestController
@RequestMapping("/api/admin/ad-metrics")
@RequiredArgsConstructor
public class AdMetricRestController {

    private static final String TABLE_NAME_DAILY = "AdMetricDailyEntry";
    private static final long D_PLUS_7_THRESHOLD_DAYS = 7;

    private final AdMetricDailyEntryRepository dailyRepository;
    private final AdMetricEntryRevisionLogRepository revisionLogRepository;
    private final AdMetricBannerEntryRepository bannerRepository;
    private final AdMetricImageEntryRepository imageRepository;
    private final AdMetricDeployMarkerRepository deployMarkerRepository;
    private final UserRepository userRepository;
    private final AdMetricAnalyticsService analyticsService;

    // ==================== 일자별 단건 조회 ====================

    @GetMapping("/daily")
    public ResponseEntity<?> get(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (date != null) {
            AdMetricDailyEntry daily = dailyRepository.findByMetricDate(date).orElse(null);
            List<AdMetricBannerEntry> banners = bannerRepository.findByMetricDate(date);
            List<AdMetricImageEntry> images = imageRepository.findByMetricDate(date);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("daily", daily);
            body.put("banners", banners);
            body.put("images", images);
            return ResponseEntity.ok(body);
        }
        if (from != null && to != null) {
            return ResponseEntity.ok(
                    dailyRepository.findByMetricDateBetweenOrderByMetricDateDesc(from, to)
            );
        }
        return ResponseEntity.badRequest().body(Map.of(
                "message", "date 또는 from/to 파라미터를 지정해주세요"
        ));
    }

    @GetMapping("/daily/{date}/revisions")
    public ResponseEntity<List<AdMetricEntryRevisionLog>> getRevisions(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(
                revisionLogRepository.findByTableNameAndRowMetricDateOrderByIdDesc(TABLE_NAME_DAILY, date)
        );
    }

    // ==================== upsert (POST/PUT/PATCH 통합) ====================

    /**
     * 통합 저장 엔드포인트 — daily 1건 + banners N건 + images N건 일괄 upsert.
     * D+7 이후: reason NOT NULL + revision log 적재.
     */
    @PostMapping("/daily")
    @Transactional
    public ResponseEntity<?> upsert(
            @RequestBody AdMetricFullRequest req,
            @AuthenticationPrincipal String adminSocialId) {

        if (req.getMetricDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "metricDate 필수"));
        }
        LocalDate date = req.getMetricDate();
        boolean afterDPlus7 = isAfterDPlus7(date);
        Long actorAdminId = resolveAdminId(adminSocialId);

        Optional<AdMetricDailyEntry> existingOpt = dailyRepository.findByMetricDate(date);
        AdMetricDailyEntry daily = existingOpt.orElseGet(() -> {
            AdMetricDailyEntry e = new AdMetricDailyEntry();
            e.setMetricDate(date);
            return e;
        });

        if (existingOpt.isPresent() && afterDPlus7
                && (req.getReason() == null || req.getReason().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "D+7 이후 수정은 사유(reason) 필수"
            ));
        }

        Map<String, Map<String, Object>> changed = existingOpt.isPresent()
                ? computeDiff(daily, req)
                : new LinkedHashMap<>();

        applyDailyRequest(daily, req);
        daily.setActorAdminId(actorAdminId);

        // race-safe upsert: UNIQUE(metric_date) 위반(=동시 INSERT) 발생 시 기존 row 다시 조회해 update.
        // GlobalExceptionHandler가 DataIntegrityViolationException를 일괄 409 "이미 처리된 요청이에요"로 변환하므로
        // 본 컨트롤러에서 명시적으로 catch하여 운영자 수정 의도를 보존한다.
        try {
            dailyRepository.save(daily);
        } catch (DataIntegrityViolationException dup) {
            AdMetricDailyEntry reloaded = dailyRepository.findByMetricDate(date)
                    .orElseThrow(() -> dup);
            applyDailyRequest(reloaded, req);
            reloaded.setActorAdminId(actorAdminId);
            dailyRepository.save(reloaded);
            daily = reloaded;
        }

        // 위치별 — 들어온 키만 upsert. 미수신 위치는 건드리지 않음.
        upsertBanners(date, req.getBanners(), actorAdminId);
        upsertImages(date, req.getImages(), actorAdminId);

        if (existingOpt.isPresent() && afterDPlus7 && !changed.isEmpty()) {
            AdMetricEntryRevisionLog log = new AdMetricEntryRevisionLog();
            log.setTableName(TABLE_NAME_DAILY);
            log.setRowMetricDate(date);
            log.setDiff(Map.of("changed", changed));
            log.setSchemaVersion(1);
            log.setReason(req.getReason());
            log.setActorAdminId(actorAdminId);
            revisionLogRepository.save(log);
        }

        return ResponseEntity.ok(Map.of(
                "message", existingOpt.isPresent() ? "수정 완료" : "신규 저장 완료",
                "afterDPlus7", afterDPlus7
        ));
    }

    private void upsertBanners(LocalDate date, List<PositionEntryRequest> reqList, Long actorAdminId) {
        if (reqList == null) return;
        List<AdMetricBannerEntry> existing = bannerRepository.findByMetricDate(date);
        Map<String, AdMetricBannerEntry> byPos = new HashMap<>();
        for (AdMetricBannerEntry e : existing) byPos.put(e.getAdPositionCode(), e);

        for (PositionEntryRequest pr : reqList) {
            if (pr.getAdPositionCode() == null || pr.getAdPositionCode().isBlank()) continue;
            AdMetricBannerEntry row = byPos.get(pr.getAdPositionCode());
            if (row == null) {
                row = new AdMetricBannerEntry();
                row.setMetricDate(date);
                row.setAdPositionCode(pr.getAdPositionCode());
            }
            if (pr.getImpression() != null) row.setImpression(pr.getImpression());
            if (pr.getCtrReported() != null) row.setCtrReported(pr.getCtrReported());
            if (pr.getEcpmReported() != null) row.setEcpmReported(pr.getEcpmReported());
            if (pr.getRevenue() != null) row.setRevenue(pr.getRevenue());
            if (pr.getAdIdSnapshot() != null) row.setAdIdSnapshot(pr.getAdIdSnapshot());
            row.setActorAdminId(actorAdminId);
            try {
                bannerRepository.save(row);
            } catch (DataIntegrityViolationException dup) {
                AdMetricBannerEntry reloaded = bannerRepository.findByMetricDate(date).stream()
                        .filter(b -> b.getAdPositionCode().equals(pr.getAdPositionCode()))
                        .findFirst().orElseThrow(() -> dup);
                if (pr.getImpression() != null) reloaded.setImpression(pr.getImpression());
                if (pr.getCtrReported() != null) reloaded.setCtrReported(pr.getCtrReported());
                if (pr.getEcpmReported() != null) reloaded.setEcpmReported(pr.getEcpmReported());
                if (pr.getRevenue() != null) reloaded.setRevenue(pr.getRevenue());
                if (pr.getAdIdSnapshot() != null) reloaded.setAdIdSnapshot(pr.getAdIdSnapshot());
                reloaded.setActorAdminId(actorAdminId);
                bannerRepository.save(reloaded);
            }
        }
    }

    private void upsertImages(LocalDate date, List<PositionEntryRequest> reqList, Long actorAdminId) {
        if (reqList == null) return;
        List<AdMetricImageEntry> existing = imageRepository.findByMetricDate(date);
        Map<String, AdMetricImageEntry> byPos = new HashMap<>();
        for (AdMetricImageEntry e : existing) byPos.put(e.getAdPositionCode(), e);

        for (PositionEntryRequest pr : reqList) {
            if (pr.getAdPositionCode() == null || pr.getAdPositionCode().isBlank()) continue;
            AdMetricImageEntry row = byPos.get(pr.getAdPositionCode());
            if (row == null) {
                row = new AdMetricImageEntry();
                row.setMetricDate(date);
                row.setAdPositionCode(pr.getAdPositionCode());
            }
            if (pr.getImpression() != null) row.setImpression(pr.getImpression());
            if (pr.getCtrReported() != null) row.setCtrReported(pr.getCtrReported());
            if (pr.getEcpmReported() != null) row.setEcpmReported(pr.getEcpmReported());
            if (pr.getRevenue() != null) row.setRevenue(pr.getRevenue());
            if (pr.getAdIdSnapshot() != null) row.setAdIdSnapshot(pr.getAdIdSnapshot());
            row.setActorAdminId(actorAdminId);
            try {
                imageRepository.save(row);
            } catch (DataIntegrityViolationException dup) {
                AdMetricImageEntry reloaded = imageRepository.findByMetricDate(date).stream()
                        .filter(b -> b.getAdPositionCode().equals(pr.getAdPositionCode()))
                        .findFirst().orElseThrow(() -> dup);
                if (pr.getImpression() != null) reloaded.setImpression(pr.getImpression());
                if (pr.getCtrReported() != null) reloaded.setCtrReported(pr.getCtrReported());
                if (pr.getEcpmReported() != null) reloaded.setEcpmReported(pr.getEcpmReported());
                if (pr.getRevenue() != null) reloaded.setRevenue(pr.getRevenue());
                if (pr.getAdIdSnapshot() != null) reloaded.setAdIdSnapshot(pr.getAdIdSnapshot());
                reloaded.setActorAdminId(actorAdminId);
                imageRepository.save(reloaded);
            }
        }
    }

    // ==================== 분석 (집계) ====================

    @GetMapping("/kpi")
    public ResponseEntity<AdMetricAnalyticsDto.Kpi> getKpi(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(analyticsService.getKpi(date));
    }

    @GetMapping("/daily-table")
    public ResponseEntity<List<AdMetricAnalyticsDto.DailyTableRow>> getDailyTable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getDailyTable(from, to));
    }

    // ==================== 배포 마커 ====================

    @GetMapping("/deploys")
    public ResponseEntity<List<AdMetricDeployMarker>> listDeploys() {
        return ResponseEntity.ok(deployMarkerRepository.findAllByOrderByMetricDateAsc());
    }

    @PostMapping("/deploys")
    @Transactional
    public ResponseEntity<?> createDeploy(
            @RequestBody DeployMarkerRequest req,
            @AuthenticationPrincipal String adminSocialId) {
        if (req.getMetricDate() == null
                || req.getDeployVersion() == null
                || req.getDeployVersion().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "metricDate, deployVersion 필수"
            ));
        }
        AdMetricDeployMarker marker = new AdMetricDeployMarker();
        marker.setMetricDate(req.getMetricDate());
        marker.setDeployVersion(req.getDeployVersion());
        marker.setDeployNote(req.getDeployNote());
        marker.setActorAdminId(resolveAdminId(adminSocialId));
        deployMarkerRepository.save(marker);
        return ResponseEntity.ok(marker);
    }

    // ==================== 헬퍼 ====================

    private boolean isAfterDPlus7(LocalDate metricDate) {
        return ChronoUnit.DAYS.between(metricDate, LocalDate.now()) >= D_PLUS_7_THRESHOLD_DAYS;
    }

    private Long resolveAdminId(String adminSocialId) {
        UserB admin = userRepository.findBySocialId(adminSocialId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다"));
        return admin.getId();
    }

    private void applyDailyRequest(AdMetricDailyEntry e, AdMetricFullRequest r) {
        if (r.getTossRevenueR() != null) e.setTossRevenueR(r.getTossRevenueR());
        if (r.getTossRevenueF() != null) e.setTossRevenueF(r.getTossRevenueF());
        if (r.getTossRevenueBTotal() != null) e.setTossRevenueBTotal(r.getTossRevenueBTotal());
        if (r.getTossRevenueI() != null) e.setTossRevenueI(r.getTossRevenueI());

        if (r.getEcpmRReported() != null) e.setEcpmRReported(r.getEcpmRReported());
        if (r.getEcpmFReported() != null) e.setEcpmFReported(r.getEcpmFReported());
        if (r.getEcpmBTotalReported() != null) e.setEcpmBTotalReported(r.getEcpmBTotalReported());
        if (r.getEcpmIReported() != null) e.setEcpmIReported(r.getEcpmIReported());

        if (r.getCtrRReported() != null) e.setCtrRReported(r.getCtrRReported());
        if (r.getCtrFReported() != null) e.setCtrFReported(r.getCtrFReported());
        if (r.getCtrBTotalReported() != null) e.setCtrBTotalReported(r.getCtrBTotalReported());
        if (r.getCtrIReported() != null) e.setCtrIReported(r.getCtrIReported());

        if (r.getImpressionRReported() != null) e.setImpressionRReported(r.getImpressionRReported());
        if (r.getImpressionFReported() != null) e.setImpressionFReported(r.getImpressionFReported());
        if (r.getImpressionBTotal() != null) e.setImpressionBTotal(r.getImpressionBTotal());
        if (r.getImpressionI() != null) e.setImpressionI(r.getImpressionI());

        if (r.getNewUsersReported() != null) e.setNewUsersReported(r.getNewUsersReported());
        if (r.getTotalUsersReported() != null) e.setTotalUsersReported(r.getTotalUsersReported());
        if (r.getRetentionD1New() != null) e.setRetentionD1New(r.getRetentionD1New());
        if (r.getRetentionD1Total() != null) e.setRetentionD1Total(r.getRetentionD1Total());
    }

    private Map<String, Map<String, Object>> computeDiff(AdMetricDailyEntry b, AdMetricFullRequest r) {
        Map<String, Map<String, Object>> changed = new LinkedHashMap<>();
        addIfChanged(changed, "tossRevenueR", b.getTossRevenueR(), r.getTossRevenueR());
        addIfChanged(changed, "tossRevenueF", b.getTossRevenueF(), r.getTossRevenueF());
        addIfChanged(changed, "tossRevenueBTotal", b.getTossRevenueBTotal(), r.getTossRevenueBTotal());
        addIfChanged(changed, "tossRevenueI", b.getTossRevenueI(), r.getTossRevenueI());

        addIfChanged(changed, "ecpmRReported", b.getEcpmRReported(), r.getEcpmRReported());
        addIfChanged(changed, "ecpmFReported", b.getEcpmFReported(), r.getEcpmFReported());
        addIfChanged(changed, "ecpmBTotalReported", b.getEcpmBTotalReported(), r.getEcpmBTotalReported());
        addIfChanged(changed, "ecpmIReported", b.getEcpmIReported(), r.getEcpmIReported());

        addIfChanged(changed, "ctrRReported", b.getCtrRReported(), r.getCtrRReported());
        addIfChanged(changed, "ctrFReported", b.getCtrFReported(), r.getCtrFReported());
        addIfChanged(changed, "ctrBTotalReported", b.getCtrBTotalReported(), r.getCtrBTotalReported());
        addIfChanged(changed, "ctrIReported", b.getCtrIReported(), r.getCtrIReported());

        addIfChanged(changed, "impressionRReported", b.getImpressionRReported(), r.getImpressionRReported());
        addIfChanged(changed, "impressionFReported", b.getImpressionFReported(), r.getImpressionFReported());
        addIfChanged(changed, "impressionBTotal", b.getImpressionBTotal(), r.getImpressionBTotal());
        addIfChanged(changed, "impressionI", b.getImpressionI(), r.getImpressionI());

        addIfChanged(changed, "newUsersReported", b.getNewUsersReported(), r.getNewUsersReported());
        addIfChanged(changed, "totalUsersReported", b.getTotalUsersReported(), r.getTotalUsersReported());
        addIfChanged(changed, "retentionD1New", b.getRetentionD1New(), r.getRetentionD1New());
        addIfChanged(changed, "retentionD1Total", b.getRetentionD1Total(), r.getRetentionD1Total());
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

    @Getter @Setter
    public static class AdMetricFullRequest {
        private LocalDate metricDate;

        // 일별 4축 (R/F/B합산/I 각각: 노출·시청률·eCPM·수익)
        private Integer tossRevenueR;
        private Integer tossRevenueF;
        private Integer tossRevenueBTotal;
        private Integer tossRevenueI;

        private Integer ecpmRReported;
        private Integer ecpmFReported;
        private Integer ecpmBTotalReported;
        private Integer ecpmIReported;

        private BigDecimal ctrRReported;
        private BigDecimal ctrFReported;
        private BigDecimal ctrBTotalReported;
        private BigDecimal ctrIReported;

        private Integer impressionRReported;
        private Integer impressionFReported;
        private Integer impressionBTotal;
        private Integer impressionI;

        // 토스 콘솔 reported — 유저 + 리텐션
        private Integer newUsersReported;
        private Integer totalUsersReported;
        private BigDecimal retentionD1New;
        private BigDecimal retentionD1Total;

        // 위치별 분해 (B + I)
        private List<PositionEntryRequest> banners;
        private List<PositionEntryRequest> images;

        // D+7 이후 수정 시 NOT NULL
        private String reason;
    }

    @Getter @Setter
    public static class DeployMarkerRequest {
        private LocalDate metricDate;
        private String deployVersion;
        private String deployNote;
    }

    @Getter @Setter
    public static class PositionEntryRequest {
        private String adPositionCode;
        private Integer impression;
        private BigDecimal ctrReported;
        private Integer ecpmReported;
        private Integer revenue;
        /** 해당일 매핑된 광고 ID — 위치별 row는 AdPositionConfig 자동, OTHER는 운영자 직접 입력 */
        private String adIdSnapshot;
    }
}
