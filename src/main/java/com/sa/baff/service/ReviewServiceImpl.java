package com.sa.baff.service;

import com.sa.baff.domain.*;
import com.sa.baff.model.dto.GoalsDto;
import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;
import com.sa.baff.repository.*;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.GoalType;
import com.sa.baff.util.RewardType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService{

    private static final int DEFAULT_COOLDOWN_MINUTES = 1440; // 24시간

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final BattleRoomRepository battleRoomRepository;
    private final WeightRepository weightRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final UserRepository userRepository;
    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final GoalsRepository goalsRepository;
    private final RewardConfigRepository rewardConfigRepository;

    @Override
    public Long createReview(ReviewVO.createReview param, String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 쿨타임 체크
        checkReviewCooldown(user);

        Review review = Review.builder()
                .title(param.getTitle())
                .dietMethods(param.getDietMethods())
                .difficulty(param.getDifficulty())
                .startWeight(param.getStartWeight())
                .targetWeight(param.getTargetWeight())
                .period(param.getPeriod())
                .question_hardest_period(param.getQuestion_hardest_period())
                .question_diet_management(param.getQuestion_diet_management())
                .question_exercise(param.getQuestion_exercise())
                .question_effective_method(param.getQuestion_effective_method())
                .question_recommend_target(param.getQuestion_recommend_target())
                .content(param.getContent())
                .imageUrl1(param.getImageUrl1())
                .imageUrl2(param.getImageUrl2())
                .isWeightPrivate(param.isWeightPrivate())
                .reviewType(param.getReviewType())
                .battleRoomEntryCode(param.getBattleRoomEntryCode())
                .goalId(param.getGoalId())
                .isPublic(param.isPublic())
                .user(user)
                .build();

        Review saved = reviewRepository.save(review);
        return saved.getId();
    }

    @Override
    public Map<String, Object> getCooldownStatus(String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        int cooldownMinutes = getReviewCooldownMinutes();
        Optional<Review> lastReview = reviewRepository.findTopByUserAndDelYnOrderByRegDateTimeDesc(user, 'N');

        Map<String, Object> result = new HashMap<>();
        if (lastReview.isPresent() && cooldownMinutes > 0) {
            LocalDateTime cooldownEnd = lastReview.get().getRegDateTime().plusMinutes(cooldownMinutes);
            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                long remainingMinutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), cooldownEnd);
                result.put("canWrite", false);
                result.put("nextAvailableAt", cooldownEnd.toString());
                result.put("remainingMinutes", remainingMinutes);
                return result;
            }
        }
        result.put("canWrite", true);
        result.put("nextAvailableAt", null);
        result.put("remainingMinutes", 0);
        return result;
    }

    private void checkReviewCooldown(UserB user) {
        int cooldownMinutes = getReviewCooldownMinutes();
        if (cooldownMinutes <= 0) return;

        Optional<Review> lastReview = reviewRepository.findTopByUserAndDelYnOrderByRegDateTimeDesc(user, 'N');
        if (lastReview.isPresent()) {
            LocalDateTime cooldownEnd = lastReview.get().getRegDateTime().plusMinutes(cooldownMinutes);
            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                long remainingMinutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), cooldownEnd);
                long hours = remainingMinutes / 60;
                long mins = remainingMinutes % 60;
                String timeStr = hours > 0 ? hours + "시간 " + mins + "분" : mins + "분";
                throw new IllegalStateException(timeStr + " 후에 다시 작성할 수 있어요.");
            }
        }
    }

    private int getReviewCooldownMinutes() {
        List<RewardConfig> configs = rewardConfigRepository.findActiveConfigs(RewardType.REVIEW);
        return configs.stream()
                .map(RewardConfig::getCooldownMinutes)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(DEFAULT_COOLDOWN_MINUTES);
    }

    @Override
    public ReviewDto.ReviewListResponse getReviewList(String socialId, int page, int size, String category) {
        // socialId가 유효하지 않은 경우(비로그인 시)를 고려하여 Optional.empty()로 시작
        Optional<UserB> userOptional = Optional.empty();

        // socialId가 있을 때만 DB 조회를 시도
        if (socialId != null && !socialId.isEmpty()) {
            userOptional = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId);
        }

        // ⭐️ 핵심 수정 부분:
        // userOptional이 비어있으면 UserB.createNonExistingUser() (ID=0)를 반환합니다.
        UserB user = userOptional
                .orElse(UserB.createNonExistingUser());

        // user.getId()는 로그인 시 실제 ID, 비로그인 시 0L을 반환합니다.
        Long userId = user.getId();

        // Repository 메서드는 이제 userId가 0L인 경우(비로그인)를 안전하게 처리해야 합니다.
        Page<ReviewDto.getReviewList> reviewPage = reviewRepository.getReviewList(page, size, userId, category);

        return new ReviewDto.ReviewListResponse(reviewPage);
    }

    @Override
    public void deleteReview(Long reviewId, String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        reviewRepository.deleteReview(reviewId, user.getId());
    }

    @Override
    public ReviewDto.getBattleDataForReview getBattleDataForReview(ReviewVO.getBattleDataForReview param) {
        // 작성자의 변화량, 상대 감량, 기간, 승자, 작성자의 목표량
        BattleRoom battleRoom = battleRoomRepository.findByEntryCode(param.getEntryCode())
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        List<BattleParticipant> participants = battleParticipantRepository.findAllByRoomAndDelYn(battleRoom, 'N');

        BattleParticipant host = participants.stream()
                .filter(p -> p.getUser().getId().equals(param.getHostId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보가 참가자 목록에 없습니다."));

        BattleParticipant opponent = participants.stream()
                .filter(p -> !p.getUser().getId().equals(param.getHostId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("상대방 정보를 찾을 수 없습니다."));

        LocalDate battleEndDate = battleRoom.getEndDate();

        LocalDateTime endOfBattleDay = battleEndDate.atStartOfDay().with(LocalTime.MAX);

        // 호스트 최종 체중: 배틀 종료일 자정까지 기록된 체중 중 가장 최신 기록을 찾습니다.
        Double hostFinalWeight = weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(host.getUser(), endOfBattleDay)
                .map(Weight::getWeight)
                .orElse(host.getStartingWeight());

        Double opponentFinalWeight = weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(opponent.getUser(), endOfBattleDay)
                .map(Weight::getWeight)
                .orElse(opponent.getStartingWeight());

        Double hostWeightChange;
        if(host.getGoalType() == GoalType.WEIGHT_LOSS) {
             hostWeightChange = host.getStartingWeight() - hostFinalWeight;
        } else if(host.getGoalType() == GoalType.WEIGHT_GAIN) {
             hostWeightChange = hostFinalWeight - host.getStartingWeight();
        } else {
             hostWeightChange = 0.0;
        }

        Double opponentWeightChange;
        if(opponent.getGoalType() == GoalType.WEIGHT_LOSS) {
            opponentWeightChange = opponent.getStartingWeight() - opponentFinalWeight;
        } else if(opponent.getGoalType() == GoalType.WEIGHT_GAIN) {
            opponentWeightChange = opponentFinalWeight - opponent.getStartingWeight();
        } else {
            opponentWeightChange = 0.0;
        }

        GoalType hostGoalType = host.getGoalType();

        Long durationDays = Long.valueOf(battleRoom.getDurationDays());

        Double hostTargetWeight = host.getTargetValue();

        return new ReviewDto.getBattleDataForReview(
                hostWeightChange, opponentWeightChange, durationDays, hostTargetWeight, hostGoalType
        );
    }

    @Override
    public GoalsDto.getGoalDetail getGoalDataForReview(ReviewVO.getGoalDataForReview param) {
        // 1. 사용자 확인 및 조회
        UserB user = userRepository.findUserIdById(param.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: "));

        // 2. 목표(Goal) 조회 및 사용자 권한 확인
        Goals goal = goalsRepository.findByIdAndUser(param.getGoalId(), user)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found or does not belong to user: "));

        // 3. 최종 체중 (현재 체중) 조회
        Optional<Weight> latestWeightOpt = weightRepository.findTopByUserOrderByRecordDateDesc(user);

        // 현재 체중 값 추출 (기록이 없으면, 안전하게 0.0 또는 목표 시작 체중 등을 기본값으로 설정할 수 있지만, 여기서는 Optional 사용)
        Double currentWeight = latestWeightOpt.map(Weight::getWeight).orElse(null);

        long durationDays = ChronoUnit.DAYS.between(
                goal.getStartDate().toLocalDate(), // 목표 시작일
                goal.getEndDate().toLocalDate()    // 목표 종료일
        ); // 종료일을 포함하기 위해 +1 (예: 1일부터 3일까지는 3일)

        // 4. DTO 객체 생성 및 매핑
        GoalsDto.getGoalDetail dto = new GoalsDto.getGoalDetail();

        // 목표 정보 매핑
        dto.setGoalsId(goal.getId());
        dto.setTitle(goal.getTitle());
        dto.setDurationDays(durationDays);
        dto.setStartWeight(goal.getStartWeight()); // 목표 시작 시의 체중
        dto.setTargetWeight(goal.getTargetWeight()); // 목표 체중
        weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(user, goal.getEndDate())
                .ifPresentOrElse(weightEnd -> dto.setCurrentWeight(weightEnd.getWeight()),
                        () -> dto.setCurrentWeight(goal.getStartWeight()));

        return dto;
    }

    @Override
    @Transactional
    public void editReview(ReviewVO.createReview param, String socialId, Long reviewId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        Review reviewToUpdate = reviewRepository.findByIdAndUserId(reviewId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Review not found or unauthorized access for reviewId: " + reviewId));

        reviewToUpdate.setTitle(param.getTitle());
        reviewToUpdate.setDietMethods(param.getDietMethods());
        reviewToUpdate.setDifficulty(param.getDifficulty());
        reviewToUpdate.setStartWeight(param.getStartWeight());
        reviewToUpdate.setTargetWeight(param.getTargetWeight());
        reviewToUpdate.setPeriod(param.getPeriod());
        reviewToUpdate.setQuestion_hardest_period(param.getQuestion_hardest_period());
        reviewToUpdate.setQuestion_diet_management(param.getQuestion_diet_management());
        reviewToUpdate.setQuestion_exercise(param.getQuestion_exercise());
        reviewToUpdate.setQuestion_effective_method(param.getQuestion_effective_method());
        reviewToUpdate.setQuestion_recommend_target(param.getQuestion_recommend_target());
        reviewToUpdate.setContent(param.getContent());
        reviewToUpdate.setImageUrl1(param.getImageUrl1());
        reviewToUpdate.setImageUrl2(param.getImageUrl2());
        reviewToUpdate.setReviewType(param.getReviewType());
        reviewToUpdate.setBattleRoomEntryCode(param.getBattleRoomEntryCode());
        reviewToUpdate.setGoalId(param.getGoalId());

        // Primitive boolean (요청에 없어도 false가 들어와 기존 true 값이 false로 덮어쓰여질 수 있습니다!)
        reviewToUpdate.setWeightPrivate(param.isWeightPrivate());
        reviewToUpdate.setIsPublic(param.isPublic());
    }
}
