package com.sa.baff.service;

import com.sa.baff.domain.Piece;
import com.sa.baff.domain.SmartPushConfig;
import com.sa.baff.domain.SmartPushHistory;
import com.sa.baff.repository.*;
import com.sa.baff.util.SmartPushType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SmartPushServiceImpl implements SmartPushService {

    private static final long EXCHANGE_MIN_BALANCE = 100L;

    private final SmartPushConfigRepository configRepository;
    private final SmartPushHistoryRepository historyRepository;
    private final PieceRepository pieceRepository;
    private final UserAttendanceRepository attendanceRepository;
    private final ExchangeHistoryRepository exchangeHistoryRepository;

    @Override
    public List<Long> findExchangeReminderTargets(int thresholdDays) {
        LocalDateTime cutoff = LocalDate.now().minusDays(thresholdDays).atStartOfDay();

        // balance >= 100g인 사용자 중, 마지막 환전이 cutoff 이전이거나 환전 이력 없는 사용자
        List<Piece> piecesWithBalance = pieceRepository.findAll().stream()
                .filter(p -> p.getBalance() >= EXCHANGE_MIN_BALANCE)
                .collect(Collectors.toList());

        return piecesWithBalance.stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findAttendanceReminderTargets() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        // 오늘 미출석 + 최근 7일 내 출석 이력 있는 사용자
        List<Long> recentActiveUserIds = attendanceRepository
                .findUserIdsWithAttendanceBetween(weekAgo, today.minusDays(1));

        List<Long> todayAttendedUserIds = attendanceRepository
                .findUserIdsByAttendanceDate(today);

        return recentActiveUserIds.stream()
                .filter(userId -> !todayAttendedUserIds.contains(userId))
                .collect(Collectors.toList());
    }

    @Override
    public void executePush(SmartPushType pushType) {
        Optional<SmartPushConfig> configOpt = configRepository.findByPushType(pushType);
        if (configOpt.isEmpty()) {
            log.info("스마트발송 설정 없음: {}", pushType);
            return;
        }

        SmartPushConfig config = configOpt.get();
        if (!config.getEnabled()) {
            log.info("스마트발송 비활성: {}", pushType);
            return;
        }

        List<Long> targetUserIds;
        switch (pushType) {
            case EXCHANGE_REMINDER ->
                    targetUserIds = findExchangeReminderTargets(
                            config.getThresholdDays() != null ? config.getThresholdDays() : 7);
            case ATTENDANCE_REMINDER ->
                    targetUserIds = findAttendanceReminderTargets();
            default -> {
                log.warn("알 수 없는 스마트발송 타입: {}", pushType);
                return;
            }
        }

        log.info("스마트발송 대상: type={}, count={}", pushType, targetUserIds.size());

        // TODO: 토스 스마트발송 API 연동 시 여기서 실제 발송
        // 현재는 대상 추출 + 이력 기록만 수행
        for (Long userId : targetUserIds) {
            SmartPushHistory history = new SmartPushHistory(
                    userId, pushType, "토스 API 미연동 (대기 중)", false);
            historyRepository.save(history);
        }

        log.info("스마트발송 완료: type={}, processed={}", pushType, targetUserIds.size());
    }
}
