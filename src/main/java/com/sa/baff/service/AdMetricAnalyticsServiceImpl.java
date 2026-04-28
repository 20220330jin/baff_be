package com.sa.baff.service;

import com.sa.baff.domain.AdMetricDailyEntry;
import com.sa.baff.model.dto.AdMetricAnalyticsDto;
import com.sa.baff.model.dto.AdminDashboardDto;
import com.sa.baff.repository.AdMetricDailyEntryRepository;
import com.sa.baff.util.AdWatchLocation;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * P0 광고전략 분석 service 구현.
 *
 * truth 룰 (spec §3-3):
 *  - R/F 노출은 DB AdWatchEvent observed = truth (콘솔 reported는 검증용)
 *  - B/I 노출은 콘솔 reported = truth (DB 미수집)
 *  - 토스수익 / eCPM은 모두 콘솔 reported = truth
 *
 * 활성유저(raw): 어뷰저 필터 미적용. 정책 결정 후 활성유저(자연)으로 rename.
 * 정의: AdWatchEvent에 1건 이상 기록한 distinct user_id (P0 단순 정의).
 *
 * 그램 단가: 1g = 1원 가정 (A-5 미정 — strategy.md 가정 그대로).
 */
@Service
@RequiredArgsConstructor
public class AdMetricAnalyticsServiceImpl implements AdMetricAnalyticsService {

    private static final long GRAM_UNIT_PRICE_KRW = 1L;

    /**
     * 리워드 광고 R observed 집계 대상.
     * 실제 enum 기준 (체인지업 빌드 활성 위치). spec strategy.md §4의 P1/P3 코드명과 매핑:
     *  - WEIGHT_AD_BONUS / ATTENDANCE_AD_BONUS / EXCHANGE: spec §2-1 P1
     *  - REVIEW_AD_BONUS / FASTING_AD_BONUS / ATTENDANCE_STREAK_SAVE / WEIGHT_RECORD_REWARD: 빌드 활성 리워드
     * spec §2-1의 ANALYSIS_UNLOCK / REVIEW_LIKE_AD_BONUS는 P3 미구현 — 추후 enum 추가 시 본 목록 갱신.
     */
    private static final List<AdWatchLocation> REWARD_LOCATIONS = List.of(
            AdWatchLocation.WEIGHT_AD_BONUS,
            AdWatchLocation.ATTENDANCE_AD_BONUS,
            AdWatchLocation.EXCHANGE,
            AdWatchLocation.WEIGHT_RECORD_REWARD,
            AdWatchLocation.REVIEW_AD_BONUS,
            AdWatchLocation.FASTING_AD_BONUS,
            AdWatchLocation.ATTENDANCE_STREAK_SAVE
    );

    private final AdMetricDailyEntryRepository dailyRepository;
    private final AdminDashboardService adminDashboardService;
    private final EntityManager entityManager;

    @Override
    public AdMetricAnalyticsDto.Kpi getKpi(LocalDate metricDate) {
        return buildKpi(metricDate);
    }

    @Override
    public List<AdMetricAnalyticsDto.DailyTableRow> getDailyTable(LocalDate from, LocalDate to) {
        List<AdMetricAnalyticsDto.DailyTableRow> rows = new ArrayList<>();
        LocalDate cursor = to;
        while (!cursor.isBefore(from)) {
            rows.add(buildDailyTableRow(cursor));
            cursor = cursor.minusDays(1);
        }
        return rows;
    }

    // ==================== 단일 일자 KPI ====================

    private AdMetricAnalyticsDto.Kpi buildKpi(LocalDate date) {
        Optional<AdMetricDailyEntry> dailyOpt = dailyRepository.findByMetricDate(date);
        AdMetricDailyEntry daily = dailyOpt.orElse(null);

        long observedR = countAdWatchByLocations(date, REWARD_LOCATIONS);
        long observedF = 0L; // 체인지업은 전면(F) 분리 미수집 — 콘솔 reported 사용

        long activeUsers = countActiveUsersRaw(date);
        long weightLog = countWeightLog(date);
        long attendance = countAttendance(date);
        long exchange = countExchange(date);
        long newSignups = countNewSignups(date);

        AdminDashboardDto.GramEconomySnapshot gram = adminDashboardService.getGramEconomySnapshot();

        Long netProfit = computeNetProfit(daily, gram);
        Integer totalEcpm = computeTotalEcpm(daily);

        return AdMetricAnalyticsDto.Kpi.builder()
                .metricDate(date)
                .netProfit(netProfit)
                .totalEcpm(totalEcpm)
                .activeUsersRaw(activeUsers)
                .coreActions(AdMetricAnalyticsDto.CoreActions.builder()
                        .weightLog(weightLog)
                        .attendance(attendance)
                        .exchange(exchange)
                        .build())
                .newSignups(newSignups)
                .cumulativeGramBalance(AdMetricAnalyticsDto.GramBalance.builder()
                        .totalIssued(gram.getTotalEarned())
                        .circulating(gram.getCirculating())
                        .holders(gram.getHoldersCount())
                        .build())
                .observedImpressionR(observedR)
                .observedImpressionF(observedF)
                .daily(daily)
                .build();
    }

    private AdMetricAnalyticsDto.DailyTableRow buildDailyTableRow(LocalDate date) {
        Optional<AdMetricDailyEntry> dailyOpt = dailyRepository.findByMetricDate(date);
        long observedR = countAdWatchByLocations(date, REWARD_LOCATIONS);
        return AdMetricAnalyticsDto.DailyTableRow.builder()
                .metricDate(date)
                .daily(dailyOpt.orElse(null))
                .observedImpressionR(observedR)
                .observedImpressionF(0L)
                .activeUsersRaw(countActiveUsersRaw(date))
                .weightLog(countWeightLog(date))
                .attendance(countAttendance(date))
                .exchange(countExchange(date))
                .newSignups(countNewSignups(date))
                .build();
    }

    // ==================== 집계 헬퍼 (JPQL) ====================

    private long countAdWatchByLocations(LocalDate date, List<AdWatchLocation> locations) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long result = entityManager.createQuery(
                        "SELECT COUNT(a) FROM AdWatchEvent a " +
                                "WHERE a.regDateTime >= :start AND a.regDateTime < :end " +
                                "AND a.watchLocation IN :locations",
                        Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("locations", locations)
                .getSingleResult();
        return result == null ? 0 : result;
    }

    /**
     * 활성유저(raw) — 어뷰저 필터 미적용.
     * 정의: 당일 AdWatchEvent OR Weight OR UserAttendance 1건 이상 distinct user_id 합집합.
     * 정책 결정 후 service 메서드 시그니처 변경하지 않고 필터 술어만 추가하면 됨.
     */
    private long countActiveUsersRaw(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        Long fromAds = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT a.userId) FROM AdWatchEvent a " +
                                "WHERE a.regDateTime >= :start AND a.regDateTime < :end",
                        Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();

        Long fromWeights = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT w.user.id) FROM Weight w " +
                                "WHERE w.recordDate >= :start AND w.recordDate < :end",
                        Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();

        Long fromAttendance = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT ua.userId) FROM UserAttendance ua " +
                                "WHERE ua.attendanceDate = :date",
                        Long.class)
                .setParameter("date", date)
                .getSingleResult();

        // 정확한 합집합은 native query 필요. P0에서는 max를 보수적 lower bound로 사용 + 메모.
        // 정책 결정 후 native UNION DISTINCT로 정확 집계.
        return Math.max(Math.max(orZero(fromAds), orZero(fromWeights)), orZero(fromAttendance));
    }

    private long countWeightLog(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long result = entityManager.createQuery(
                        "SELECT COUNT(w) FROM Weight w " +
                                "WHERE w.recordDate >= :start AND w.recordDate < :end",
                        Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        return orZero(result);
    }

    private long countAttendance(LocalDate date) {
        Long result = entityManager.createQuery(
                        "SELECT COUNT(ua) FROM UserAttendance ua " +
                                "WHERE ua.attendanceDate = :date",
                        Long.class)
                .setParameter("date", date)
                .getSingleResult();
        return orZero(result);
    }

    private long countExchange(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long result = entityManager.createQuery(
                        "SELECT COUNT(eh) FROM ExchangeHistory eh " +
                                "WHERE eh.regDateTime >= :start AND eh.regDateTime < :end",
                        Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        return orZero(result);
    }

    private long countNewSignups(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long result = entityManager.createQuery(
                        "SELECT COUNT(u) FROM UserB u " +
                                "WHERE u.regDateTime >= :start AND u.regDateTime < :end",
                        Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        return orZero(result);
    }

    // ==================== KPI 산식 ====================

    private Long computeNetProfit(AdMetricDailyEntry daily, AdminDashboardDto.GramEconomySnapshot gram) {
        if (daily == null) return null;
        long totalRevenue = sumNullable(
                daily.getTossRevenueR(),
                daily.getTossRevenueF(),
                daily.getTossRevenueBTotal(),
                daily.getTossRevenueI()
        );
        long totalCost = gram.getTodayIssued() * GRAM_UNIT_PRICE_KRW;
        // 토스포인트직접지급은 P1 입력 필드(현재 미수집) — 0 가정
        return totalRevenue - totalCost;
    }

    private Integer computeTotalEcpm(AdMetricDailyEntry daily) {
        if (daily == null) return null;
        Integer r = daily.getEcpmRReported();
        Integer f = daily.getEcpmFReported();
        Integer b = daily.getEcpmBTotalReported();
        Integer i = daily.getEcpmIReported();
        if (r == null && f == null && b == null && i == null) return null;
        return orZero(r) + orZero(f) + orZero(b) + orZero(i);
    }

    // ==================== 유틸 ====================

    private static long sumNullable(Integer... values) {
        long sum = 0;
        for (Integer v : values) {
            if (v != null) sum += v;
        }
        return sum;
    }

    private static long orZero(Long v) {
        return v == null ? 0L : v;
    }

    private static int orZero(Integer v) {
        return v == null ? 0 : v;
    }
}
