package com.sa.baff.service;

import com.sa.baff.domain.*;
import com.sa.baff.model.dto.MissionDto;
import com.sa.baff.repository.*;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MissionServiceImpl implements MissionService {

    // S6-8: 대표님 지시 (2026-04-22) — 출석 3일, 체중기록 3일 (기존 출석 4 → 3)
    private static final int WEEKLY_ATTENDANCE_TARGET = 3;
    private static final int WEEKLY_WEIGHT_LOG_TARGET = 3;
    private static final int DEFAULT_MISSION_REWARD = 5;

    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final WeeklyMissionProgressRepository missionRepository;
    private final PieceRepository pieceRepository;
    private final PieceTransactionRepository pieceTransactionRepository;
    private final RewardConfigRepository rewardConfigRepository;
    private final RewardHistoryRepository rewardHistoryRepository;

    private final Random random = new Random();

    @Override
    public MissionDto.weeklyStatusResponse getWeeklyMissionStatus(String socialId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();
        LocalDate weekStart = getWeekStart();
        LocalDate weekEnd = weekStart.plusDays(6);

        List<WeeklyMissionProgress> progressList = missionRepository.findByUserIdAndWeekStartDate(userId, weekStart);

        // lazy init: 미션이 없으면 자동 생성
        if (progressList.isEmpty()) {
            progressList = initWeeklyMissions(userId, weekStart);
        }

        List<MissionDto.missionItem> items = new ArrayList<>();
        for (WeeklyMissionProgress p : progressList) {
            int rewardAmount = getRewardAmount(p.getMissionType());
            items.add(MissionDto.missionItem.builder()
                    .missionType(p.getMissionType().name())
                    .title(getMissionTitle(p.getMissionType()))
                    .description(getMissionDescription(p.getMissionType(), p.getTargetCount()))
                    .currentCount(p.getCurrentCount())
                    .targetCount(p.getTargetCount())
                    .completed(p.getCompleted())
                    .rewardClaimed(p.getRewardClaimed())
                    .rewardAmount(rewardAmount)
                    .build());
        }

        return MissionDto.weeklyStatusResponse.builder()
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .missions(items)
                .build();
    }

    @Override
    public void incrementMissionProgress(Long userId, MissionType missionType) {
        LocalDate weekStart = getWeekStart();

        Optional<WeeklyMissionProgress> opt = missionRepository
                .findByUserIdAndWeekStartDateAndMissionType(userId, weekStart, missionType);

        WeeklyMissionProgress progress;
        if (opt.isPresent()) {
            progress = opt.get();
        } else {
            // 미션이 아직 생성되지 않았으면 생성
            int target = missionType == MissionType.WEEKLY_ATTENDANCE
                    ? WEEKLY_ATTENDANCE_TARGET : WEEKLY_WEIGHT_LOG_TARGET;
            progress = new WeeklyMissionProgress(userId, weekStart, missionType, target);
        }

        if (!progress.getCompleted()) {
            progress.increment();
            missionRepository.save(progress);
            log.info("미션 진행: userId={}, type={}, count={}/{}",
                    userId, missionType, progress.getCurrentCount(), progress.getTargetCount());
        }
    }

    @Override
    public MissionDto.claimResponse claimMissionReward(String socialId, MissionType missionType) {
        UserB user = findUser(socialId);
        Long userId = user.getId();
        LocalDate weekStart = getWeekStart();

        WeeklyMissionProgress progress = missionRepository
                .findByUserIdAndWeekStartDateAndMissionType(userId, weekStart, missionType)
                .orElseThrow(() -> new IllegalStateException("미션을 찾을 수 없어요."));

        if (!progress.getCompleted()) {
            throw new IllegalStateException("미션이 아직 완료되지 않았어요.");
        }
        if (progress.getRewardClaimed()) {
            throw new IllegalStateException("이미 보상을 수령했어요.");
        }

        int earnedGrams = getRewardAmount(missionType);

        // 포인트 적립
        Piece piece = pieceRepository.findByUser(user)
                .orElseGet(() -> pieceRepository.save(new Piece(user)));
        piece.addReward((long) earnedGrams);
        pieceRepository.save(piece);

        PieceTransaction tx = PieceTransaction.builder()
                .user(user)
                .amount((long) earnedGrams)
                .type(PieceTransactionType.REWARD_MISSION)
                .build();
        pieceTransactionRepository.save(tx);

        RewardType rewardType = missionType == MissionType.WEEKLY_ATTENDANCE
                ? RewardType.MISSION_ATTENDANCE_WEEKLY : RewardType.MISSION_WEIGHT_WEEKLY;
        RewardHistory history = new RewardHistory(userId, rewardType, earnedGrams, RewardStatus.SUCCESS, null);
        rewardHistoryRepository.save(history);

        progress.setRewardClaimed(true);
        missionRepository.save(progress);

        log.info("미션 보상 수령: userId={}, type={}, grams={}", userId, missionType, earnedGrams);

        return MissionDto.claimResponse.builder()
                .earnedGrams(earnedGrams)
                .message(earnedGrams + "g을 받았어요!")
                .build();
    }

    // === private helpers ===

    private UserB findUser(String socialId) {
        return accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private LocalDate getWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private List<WeeklyMissionProgress> initWeeklyMissions(Long userId, LocalDate weekStart) {
        WeeklyMissionProgress att = new WeeklyMissionProgress(
                userId, weekStart, MissionType.WEEKLY_ATTENDANCE, WEEKLY_ATTENDANCE_TARGET);
        WeeklyMissionProgress weight = new WeeklyMissionProgress(
                userId, weekStart, MissionType.WEEKLY_WEIGHT_LOG, WEEKLY_WEIGHT_LOG_TARGET);
        missionRepository.save(att);
        missionRepository.save(weight);
        return List.of(att, weight);
    }

    private int getRewardAmount(MissionType missionType) {
        RewardType rt = missionType == MissionType.WEEKLY_ATTENDANCE
                ? RewardType.MISSION_ATTENDANCE_WEEKLY : RewardType.MISSION_WEIGHT_WEEKLY;
        List<RewardConfig> configs = rewardConfigRepository.findActiveConfigs(rt);
        if (!configs.isEmpty()) {
            return configs.get(0).getAmount();
        }
        return DEFAULT_MISSION_REWARD;
    }

    private String getMissionTitle(MissionType type) {
        return switch (type) {
            case WEEKLY_ATTENDANCE -> "출석체크";
            case WEEKLY_WEIGHT_LOG -> "체중 기록";
        };
    }

    private String getMissionDescription(MissionType type, int target) {
        return switch (type) {
            case WEEKLY_ATTENDANCE -> "이번주 " + target + "일 이상 출석하기";
            case WEEKLY_WEIGHT_LOG -> "이번주 " + target + "일 이상 체중 기록하기";
        };
    }
}
