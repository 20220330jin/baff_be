package com.sa.baff.api;

import com.sa.baff.domain.AdMetricBannerEntry;
import com.sa.baff.domain.AdMetricDailyEntry;
import com.sa.baff.domain.AdMetricDeployMarker;
import com.sa.baff.domain.AdMetricEntryRevisionLog;
import com.sa.baff.domain.AdMetricImageEntry;
import com.sa.baff.domain.AdMetricInterstitialEntry;
import com.sa.baff.domain.AdMetricRewardEntry;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.AdMetricAnalyticsDto;
import com.sa.baff.repository.AdMetricBannerEntryRepository;
import com.sa.baff.repository.AdMetricDailyEntryRepository;
import com.sa.baff.repository.AdMetricDeployMarkerRepository;
import com.sa.baff.repository.AdMetricEntryRevisionLogRepository;
import com.sa.baff.repository.AdMetricImageEntryRepository;
import com.sa.baff.repository.AdMetricInterstitialEntryRepository;
import com.sa.baff.repository.AdMetricRewardEntryRepository;
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
    private final AdMetricRewardEntryRepository rewardRepository;
    private final AdMetricInterstitialEntryRepository interstitialRepository;
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
            List<AdMetricRewardEntry> rewards = rewardRepository.findByMetricDate(date);
            List<AdMetricInterstitialEntry> interstitials = interstitialRepository.findByMetricDate(date);
            if (daily != null) {
                applyPositionSumsFromLists(daily, banners, images, rewards, interstitials);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("daily", daily);
            body.put("banners", banners);
            body.put("images", images);
            body.put("rewards", rewards);
            body.put("interstitials", interstitials);
            return ResponseEntity.ok(body);
        }
        if (from != null && to != null) {
            List<AdMetricDailyEntry> rows =
                    dailyRepository.findByMetricDateBetweenOrderByMetricDateDesc(from, to);
            // 위치별 row 합산을 daily의 B/I 합산 필드에 자동 채움 (응답 전용, persist X)
            for (AdMetricDailyEntry row : rows) {
                applyPositionSums(row);
            }
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
        upsertRewards(date, req.getRewards(), actorAdminId);
        upsertInterstitials(date, req.getInterstitials(), actorAdminId);

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

    private void upsertRewards(LocalDate date, List<PositionEntryRequest> reqList, Long actorAdminId) {
        if (reqList == null) return;
        List<AdMetricRewardEntry> existing = rewardRepository.findByMetricDate(date);
        Map<String, AdMetricRewardEntry> byPos = new HashMap<>();
        for (AdMetricRewardEntry e : existing) byPos.put(e.getAdPositionCode(), e);

        for (PositionEntryRequest pr : reqList) {
            if (pr.getAdPositionCode() == null || pr.getAdPositionCode().isBlank()) continue;
            AdMetricRewardEntry row = byPos.get(pr.getAdPositionCode());
            if (row == null) {
                row = new AdMetricRewardEntry();
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
                rewardRepository.save(row);
            } catch (DataIntegrityViolationException dup) {
                AdMetricRewardEntry reloaded = rewardRepository.findByMetricDate(date).stream()
                        .filter(b -> b.getAdPositionCode().equals(pr.getAdPositionCode()))
                        .findFirst().orElseThrow(() -> dup);
                if (pr.getImpression() != null) reloaded.setImpression(pr.getImpression());
                if (pr.getCtrReported() != null) reloaded.setCtrReported(pr.getCtrReported());
                if (pr.getEcpmReported() != null) reloaded.setEcpmReported(pr.getEcpmReported());
                if (pr.getRevenue() != null) reloaded.setRevenue(pr.getRevenue());
                if (pr.getAdIdSnapshot() != null) reloaded.setAdIdSnapshot(pr.getAdIdSnapshot());
                reloaded.setActorAdminId(actorAdminId);
                rewardRepository.save(reloaded);
            }
        }
    }

    private void upsertInterstitials(LocalDate date, List<PositionEntryRequest> reqList, Long actorAdminId) {
        if (reqList == null) return;
        List<AdMetricInterstitialEntry> existing = interstitialRepository.findByMetricDate(date);
        Map<String, AdMetricInterstitialEntry> byPos = new HashMap<>();
        for (AdMetricInterstitialEntry e : existing) byPos.put(e.getAdPositionCode(), e);

        for (PositionEntryRequest pr : reqList) {
            if (pr.getAdPositionCode() == null || pr.getAdPositionCode().isBlank()) continue;
            AdMetricInterstitialEntry row = byPos.get(pr.getAdPositionCode());
            if (row == null) {
                row = new AdMetricInterstitialEntry();
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
                interstitialRepository.save(row);
            } catch (DataIntegrityViolationException dup) {
                AdMetricInterstitialEntry reloaded = interstitialRepository.findByMetricDate(date).stream()
                        .filter(b -> b.getAdPositionCode().equals(pr.getAdPositionCode()))
                        .findFirst().orElseThrow(() -> dup);
                if (pr.getImpression() != null) reloaded.setImpression(pr.getImpression());
                if (pr.getCtrReported() != null) reloaded.setCtrReported(pr.getCtrReported());
                if (pr.getEcpmReported() != null) reloaded.setEcpmReported(pr.getEcpmReported());
                if (pr.getRevenue() != null) reloaded.setRevenue(pr.getRevenue());
                if (pr.getAdIdSnapshot() != null) reloaded.setAdIdSnapshot(pr.getAdIdSnapshot());
                reloaded.setActorAdminId(actorAdminId);
                interstitialRepository.save(reloaded);
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

    // ==================== 위치별 합산 응답 보정 ====================

    /**
     * 응답 직전에 daily의 B/I 합산 필드를 위치별 row 합으로 채움.
     * persist 호출 X, 단순 메모리 setter (Jackson 직렬화 직전).
     */
    private void applyPositionSums(AdMetricDailyEntry daily) {
        List<AdMetricBannerEntry> banners = bannerRepository.findByMetricDate(daily.getMetricDate());
        List<AdMetricImageEntry> images = imageRepository.findByMetricDate(daily.getMetricDate());
        List<AdMetricRewardEntry> rewards = rewardRepository.findByMetricDate(daily.getMetricDate());
        List<AdMetricInterstitialEntry> interstitials = interstitialRepository.findByMetricDate(daily.getMetricDate());
        applyPositionSumsFromLists(daily, banners, images, rewards, interstitials);
    }

    private void applyPositionSumsFromLists(AdMetricDailyEntry daily,
                                            List<AdMetricBannerEntry> banners,
                                            List<AdMetricImageEntry> images,
                                            List<AdMetricRewardEntry> rewards,
                                            List<AdMetricInterstitialEntry> interstitials) {
        // B 합산 — 위치별 입력 있으면 그 합으로, 운영자가 daily에 직접 입력한 값이 있으면 우선 보존
        if (!banners.isEmpty()) {
            int impSum = 0, revSum = 0;
            double weightedCtrNum = 0, weightedCtrDen = 0;
            double weightedEcpmNum = 0, weightedEcpmDen = 0;
            for (AdMetricBannerEntry b : banners) {
                int imp = b.getImpression() == null ? 0 : b.getImpression();
                int rev = b.getRevenue() == null ? 0 : b.getRevenue();
                impSum += imp;
                revSum += rev;
                if (imp > 0 && b.getCtrReported() != null) {
                    weightedCtrNum += b.getCtrReported().doubleValue() * imp;
                    weightedCtrDen += imp;
                }
                if (imp > 0 && b.getEcpmReported() != null) {
                    weightedEcpmNum += b.getEcpmReported() * imp;
                    weightedEcpmDen += imp;
                }
            }
            if (daily.getImpressionBTotal() == null) daily.setImpressionBTotal(impSum);
            if (daily.getTossRevenueBTotal() == null) daily.setTossRevenueBTotal(revSum);
            if (daily.getCtrBTotalReported() == null && weightedCtrDen > 0) {
                daily.setCtrBTotalReported(java.math.BigDecimal.valueOf(weightedCtrNum / weightedCtrDen)
                        .setScale(2, java.math.RoundingMode.HALF_UP));
            }
            if (daily.getEcpmBTotalReported() == null && weightedEcpmDen > 0) {
                daily.setEcpmBTotalReported((int) Math.round(weightedEcpmNum / weightedEcpmDen));
            }
        }
        // I 합산 — 동일 로직
        if (!images.isEmpty()) {
            int impSum = 0, revSum = 0;
            double weightedCtrNum = 0, weightedCtrDen = 0;
            double weightedEcpmNum = 0, weightedEcpmDen = 0;
            for (AdMetricImageEntry i : images) {
                int imp = i.getImpression() == null ? 0 : i.getImpression();
                int rev = i.getRevenue() == null ? 0 : i.getRevenue();
                impSum += imp;
                revSum += rev;
                if (imp > 0 && i.getCtrReported() != null) {
                    weightedCtrNum += i.getCtrReported().doubleValue() * imp;
                    weightedCtrDen += imp;
                }
                if (imp > 0 && i.getEcpmReported() != null) {
                    weightedEcpmNum += i.getEcpmReported() * imp;
                    weightedEcpmDen += imp;
                }
            }
            if (daily.getImpressionI() == null) daily.setImpressionI(impSum);
            if (daily.getTossRevenueI() == null) daily.setTossRevenueI(revSum);
            if (daily.getCtrIReported() == null && weightedCtrDen > 0) {
                daily.setCtrIReported(java.math.BigDecimal.valueOf(weightedCtrNum / weightedCtrDen)
                        .setScale(2, java.math.RoundingMode.HALF_UP));
            }
            if (daily.getEcpmIReported() == null && weightedEcpmDen > 0) {
                daily.setEcpmIReported((int) Math.round(weightedEcpmNum / weightedEcpmDen));
            }
        }
        // R 합산 — 위치별 입력 있으면 daily 미입력 필드를 위치 합으로 자동 채움
        if (!rewards.isEmpty()) {
            int impSum = 0, revSum = 0;
            double weightedCtrNum = 0, weightedCtrDen = 0;
            double weightedEcpmNum = 0, weightedEcpmDen = 0;
            for (AdMetricRewardEntry r : rewards) {
                int imp = r.getImpression() == null ? 0 : r.getImpression();
                int rev = r.getRevenue() == null ? 0 : r.getRevenue();
                impSum += imp;
                revSum += rev;
                if (imp > 0 && r.getCtrReported() != null) {
                    weightedCtrNum += r.getCtrReported().doubleValue() * imp;
                    weightedCtrDen += imp;
                }
                if (imp > 0 && r.getEcpmReported() != null) {
                    weightedEcpmNum += r.getEcpmReported() * imp;
                    weightedEcpmDen += imp;
                }
            }
            if (daily.getImpressionRReported() == null) daily.setImpressionRReported(impSum);
            if (daily.getTossRevenueR() == null) daily.setTossRevenueR(revSum);
            if (daily.getCtrRReported() == null && weightedCtrDen > 0) {
                daily.setCtrRReported(java.math.BigDecimal.valueOf(weightedCtrNum / weightedCtrDen)
                        .setScale(2, java.math.RoundingMode.HALF_UP));
            }
            if (daily.getEcpmRReported() == null && weightedEcpmDen > 0) {
                daily.setEcpmRReported((int) Math.round(weightedEcpmNum / weightedEcpmDen));
            }
        }
        // F 합산 — 동일 로직
        if (!interstitials.isEmpty()) {
            int impSum = 0, revSum = 0;
            double weightedCtrNum = 0, weightedCtrDen = 0;
            double weightedEcpmNum = 0, weightedEcpmDen = 0;
            for (AdMetricInterstitialEntry f : interstitials) {
                int imp = f.getImpression() == null ? 0 : f.getImpression();
                int rev = f.getRevenue() == null ? 0 : f.getRevenue();
                impSum += imp;
                revSum += rev;
                if (imp > 0 && f.getCtrReported() != null) {
                    weightedCtrNum += f.getCtrReported().doubleValue() * imp;
                    weightedCtrDen += imp;
                }
                if (imp > 0 && f.getEcpmReported() != null) {
                    weightedEcpmNum += f.getEcpmReported() * imp;
                    weightedEcpmDen += imp;
                }
            }
            if (daily.getImpressionFReported() == null) daily.setImpressionFReported(impSum);
            if (daily.getTossRevenueF() == null) daily.setTossRevenueF(revSum);
            if (daily.getCtrFReported() == null && weightedCtrDen > 0) {
                daily.setCtrFReported(java.math.BigDecimal.valueOf(weightedCtrNum / weightedCtrDen)
                        .setScale(2, java.math.RoundingMode.HALF_UP));
            }
            if (daily.getEcpmFReported() == null && weightedEcpmDen > 0) {
                daily.setEcpmFReported((int) Math.round(weightedEcpmNum / weightedEcpmDen));
            }
        }
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

        // 위치별 분해 (B + I + R + F)
        private List<PositionEntryRequest> banners;
        private List<PositionEntryRequest> images;
        private List<PositionEntryRequest> rewards;
        private List<PositionEntryRequest> interstitials;

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
