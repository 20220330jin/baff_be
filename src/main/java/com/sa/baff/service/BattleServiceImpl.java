package com.sa.baff.service;

import com.sa.baff.domain.BattleParticipant;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.Weight;
import com.sa.baff.model.dto.BattleRoomDto;
import com.sa.baff.model.vo.BattleRoomVO;
import com.sa.baff.repository.BattleParticipantRepository;
import com.sa.baff.repository.BattleRoomRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.repository.WeightRepository;
import com.sa.baff.util.BattleStatus;
import com.sa.baff.util.DateTimeUtils;
import com.sa.baff.util.GoalType;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional

public class BattleServiceImpl implements BattleService {

    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final UserRepository userRepository;
    private final WeightRepository weightRepository;

    @Override
    public void createBattleRoom(BattleRoomVO.createBattleRoom createBattleRoomParam) {
        UserB host = userRepository.findUserIdBySocialId(createBattleRoomParam.getSocialId()).orElseThrow(() -> new EntityNotFoundException("User not found"));

        String entryCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        BattleRoom battleRoom = BattleRoom.builder()
                .name(createBattleRoomParam.getName())
                .password(createBattleRoomParam.getPassword())
                .description(createBattleRoomParam.getDescription())
                .maxParticipants(createBattleRoomParam.getMaxParticipants())
                .host(host)
                .status(BattleStatus.WAITING)
                .durationDays(createBattleRoomParam.getDurationDays())
                .entryCode(entryCode)
                .build();

        BattleRoom saveRoom = battleRoomRepository.save(battleRoom);

        BattleParticipant participant = BattleParticipant.builder()
                .user(host)
                .room(saveRoom)
                .build();
        battleParticipantRepository.save(participant);
    }

    @Override
    public List<BattleRoomDto.getBattleRoomList> getBattleRoomList(String socialId) {
        UserB user = userRepository.findUserIdBySocialId(socialId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        /**
         * 배틀 목록을 조회하기 전, 만료된 배틀의 상태를 업데이트
         */
        checkAndEndBattles(user);

        // delYn이 'N'이고 방도 삭제되지 않은 유효한 참가자 목록만 가져옵니다.
        List<BattleParticipant> participants = battleParticipantRepository.findAllByUserAndDelYnAndRoomDelYn(user, 'N', 'N');

        Collections.reverse(participants);

        List<BattleRoomDto.getBattleRoomList> battleRoomList = participants.stream()
                .filter(participant -> participant.getRoom().getStatus() == BattleStatus.WAITING)
                .map(participant -> {
                    BattleRoom battleRoom = participant.getRoom();

                    long currentParticipants = battleParticipantRepository.findAllByRoomAndDelYn(battleRoom, 'N').size();


                    BattleRoomDto.getBattleRoomList getBattleRoomList = new BattleRoomDto.getBattleRoomList();

                    getBattleRoomList.setName(battleRoom.getName());
                    getBattleRoomList.setPassword(battleRoom.getPassword());
                    getBattleRoomList.setDescription(battleRoom.getDescription());
                    getBattleRoomList.setHostId(battleRoom.getHost().getId());
                    getBattleRoomList.setHostNickName(battleRoom.getHost().getNickname());
                    getBattleRoomList.setStatus(battleRoom.getStatus());
                    getBattleRoomList.setMaxParticipant(battleRoom.getMaxParticipants());
                    getBattleRoomList.setCurrentParticipant((int) currentParticipants);
                    getBattleRoomList.setDurationDays(battleRoom.getDurationDays());
                    getBattleRoomList.setStartDate(battleRoom.getStartDate());
                    getBattleRoomList.setEndDate(battleRoom.getEndDate());
                    getBattleRoomList.setEntryCode(battleRoom.getEntryCode());

                    return getBattleRoomList;
                }).collect(Collectors.toList());

        return battleRoomList;
    }

    @Override
    public void joinBattleRoom(String entryCode, String password, String socialId) {
        UserB user = userRepository.findBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. entryCode로 방 찾기
        BattleRoom battleRoom = battleRoomRepository.findByEntryCode(entryCode)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        // 2. 방 상태 확인
        if (battleRoom.getStatus() != BattleStatus.WAITING) {
            throw new IllegalStateException("대결이 이미 시작되었거나 종료되었습니다.");
        }

        // 3. 비밀번호 확인
        if (battleRoom.getPassword() != null && !battleRoom.getPassword().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        // 4. 최대 인원수 확인
        if (battleRoom.getParticipants().size() >= battleRoom.getMaxParticipants()) {
            throw new IllegalStateException("방의 최대 인원이 초과되었습니다.");
        }

        // 5. 중복 참여 방지
        Optional<BattleParticipant> existingParticipant = battleParticipantRepository.findByRoomAndUser(battleRoom, user);
        if (existingParticipant.isPresent()) {
            throw new IllegalStateException("이미 참가한 방입니다.");
        }

        // 6. BattleParticipant 엔티티 생성 및 저장
        BattleParticipant participant = BattleParticipant.builder()
                .user(user)
                .room(battleRoom)
                .build();
        battleParticipantRepository.save(participant);
    }

    @Override
    public BattleRoomDto.getBattleRoomDetails.battleRoomDetail getBattleRoomDetails(String entryCode) {
        BattleRoom battleRoom = battleRoomRepository.findByEntryCode(entryCode)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        // Repository에서 delYn이 'N'인 활성 참가자만 조회
        List<BattleParticipant> participants = battleParticipantRepository.findAllByRoomAndDelYn(battleRoom, 'N');

        List<BattleRoomDto.getBattleRoomDetails.ParticipantInfo> participantInfoList = participants.stream()
                .map(participant -> {
                    BattleRoomDto.getBattleRoomDetails.ParticipantInfo info = new BattleRoomDto.getBattleRoomDetails.ParticipantInfo();
                    info.setUserNickname(participant.getUser().getNickname());
                    info.setUserId(participant.getUser().getId());
                    info.setStartingWeight(participant.getStartingWeight());

                    // Optional<Weight> 객체에서 getWeight() 메서드를호출
                    Double currentWeight = weightRepository.findFirstByUserOrderByRecordDateDesc(participant.getUser())
                            .map(Weight::getWeight)
                            .orElse(participant.getStartingWeight());
                    info.setCurrentWeight(currentWeight);

                    double progress = 0.0;
                    if (participant.getStartingWeight() != null && participant.getStartingWeight() > 0) {
                        progress = ((participant.getStartingWeight() - currentWeight) / participant.getStartingWeight()) * 100;
                    }
                    info.setProgress(progress);

                    info.setGoalType(participant.getGoalType());
                    info.setTargetValue(participant.getTargetValue());
                    info.setReady(participant.isReady());

                    return info;
                })
                .sorted(Comparator.comparingDouble(BattleRoomDto.getBattleRoomDetails.ParticipantInfo::getProgress).reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < participantInfoList.size(); i++) {
            participantInfoList.get(i).setRank(i + 1);
        }

        BattleRoomDto.getBattleRoomDetails.battleRoomDetail response = new BattleRoomDto.getBattleRoomDetails.battleRoomDetail();
        response.setName(battleRoom.getName());
        response.setDescription(battleRoom.getDescription());
        response.setStatus(battleRoom.getStatus());
        response.setMaxParticipants(battleRoom.getMaxParticipants());
        response.setCurrentParticipants(participants.size());
        response.setDurationDays(battleRoom.getDurationDays());
        response.setStartDate(battleRoom.getStartDate());
        response.setEndDate(battleRoom.getEndDate());
        response.setEntryCode(battleRoom.getEntryCode());
        response.setParticipants(participantInfoList);
        response.setHostId(battleRoom.getHost().getId());

        return response;
    }

    @Override
    public void battleGoalSetting(String entryCode, BattleRoomVO.battleGoalSetting battleGoalSetting, String socialId) {
        UserB user = userRepository.findBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. entryCode로 방 찾기
        BattleRoom battleRoom = battleRoomRepository.findByEntryCode(entryCode)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        // 2. 해당 방의 참가자(BattleParticipant)를 찾아옴
        BattleParticipant participant = battleParticipantRepository.findByRoomAndUserAndDelYn(battleRoom, user, 'N')
                .orElseThrow(() -> new IllegalArgumentException("참가자를 찾을 수 없습니다."));

        // 3. 목표 정보와 시작 체중 업데이트
        participant.setGoalType(battleGoalSetting.getGoalType());
        participant.setTargetValue(battleGoalSetting.getTargetValue());

        // 시작 체중 (startingWeight) 설정 - 목표 설정하는날 기준 가장 최신의 체중 기록 기준
        Double startingWeight = weightRepository.findFirstByUserOrderByRecordDateDesc(user)
                .map(Weight::getWeight)
                .orElseThrow(() -> new IllegalArgumentException("시작 체중 기록이 없습니다."));

        participant.setStartingWeight(startingWeight);

        // 4. 준비 상태(isReady)를 true로 변경
        participant.setReady(true);
    }

    @Override
    public void battleStart(String entryCode, String socialId) {
        UserB user = userRepository.findBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        BattleRoom battleRoom = battleRoomRepository.findByEntryCode(entryCode)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        LocalDate now = DateTimeUtils.now().toLocalDate();
        LocalDate endDate = now.plusDays(battleRoom.getDurationDays());

        battleRoom.setStatus(BattleStatus.IN_PROGRESS);
        battleRoom.setStartDate(now);
        battleRoom.setEndDate(endDate);
    }

    /**
     * 진행 중인 배틀 목록을 조회하고, 필요한 모든 데이터를 계산하여 반환
     */
    @Override
    public BattleRoomDto.ActiveBattleData activeBattles(String socialId) {
        UserB user = userRepository.findUserIdBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        /**
         * 배틀 목록을 조회하기 전, 만료된 배틀의 상태를 업데이트
         */
        checkAndEndBattles(user);

        // delYn이 'N'인 유효한 참가자만 가져옵니다.
        List<BattleParticipant> participants = battleParticipantRepository.findAllByUserAndDelYnAndRoomDelYn(user, 'N', 'N');

        Collections.reverse(participants);

        List<BattleRoomDto.BattleSummaryData> battleSummaryData = participants.stream()
                .filter(p -> p.getRoom().getStatus() == BattleStatus.IN_PROGRESS)
                .map(participant -> {
                    BattleRoom battleRoom = participant.getRoom();

                    // 상대 참가자도 delYn이 'N'인 경우만 찾습니다.
                    BattleParticipant opponentParticipant = battleParticipantRepository.findByRoomAndUserNotAndDelYn(battleRoom, user, 'N')
                            .orElseThrow(() -> new IllegalArgumentException("상대방이 없습니다."));

                    // 현재 사용자와 상대방의 최신 체중을 조회 (LocalDate를 LocalDateTime으로 변환하기 위해 atStartOfDay 사용)
                    Double myCurrentWeight = weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(user, battleRoom.getEndDate().atStartOfDay())
                            .map(Weight::getWeight)
                            .orElse(participant.getStartingWeight());

                    Double opponentCurrentWeight = weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(opponentParticipant.getUser(), battleRoom.getEndDate().atStartOfDay())
                            .map(Weight::getWeight)
                            .orElse(opponentParticipant.getStartingWeight());

                    double myProgress = 0.0;
                    double myWeightChange = 0.0;
                    GoalType myGoalType = participant.getGoalType();

                    if (myGoalType == GoalType.WEIGHT_LOSS || myGoalType == GoalType.PERCENTAGE_LOSS) {
                        myWeightChange = participant.getStartingWeight() - myCurrentWeight;
                        Double myTargetWeightLoss = calculateTargetWeightLoss(participant);
                        if (myTargetWeightLoss != null && myTargetWeightLoss > 0) {
                            myProgress = (myWeightChange / myTargetWeightLoss) * 100;
                        }
                    } else if (myGoalType == GoalType.WEIGHT_GAIN) {
                        myWeightChange = myCurrentWeight - participant.getStartingWeight();
                        if (participant.getTargetValue() != null && participant.getTargetValue() > 0) {
                            myProgress = (myWeightChange / participant.getTargetValue()) * 100;
                        }
                    } else if (myGoalType == GoalType.MAINTAIN) {
                        // 유지 목표는 시작 체중과의 차이가 적을수록 높은 점수를 받습니다.
                        // 음수 값으로 계산하여 0에 가까울수록 좋은 결과가 되도록 합니다.
                        myWeightChange = myCurrentWeight - participant.getStartingWeight();
                        myProgress = -Math.abs(myWeightChange);
                    }

                    double opponentProgress = 0.0;
                    double opponentWeightChange = 0.0;
                    GoalType opponentGoalType = opponentParticipant.getGoalType();

                    if (opponentGoalType == GoalType.WEIGHT_LOSS || opponentGoalType == GoalType.PERCENTAGE_LOSS) {
                        opponentWeightChange = opponentParticipant.getStartingWeight() - opponentCurrentWeight;
                        Double opponentTargetWeightLoss = calculateTargetWeightLoss(opponentParticipant);
                        if (opponentTargetWeightLoss != null && opponentTargetWeightLoss > 0) {
                            opponentProgress = (opponentWeightChange / opponentTargetWeightLoss) * 100;
                        }
                    } else if (opponentGoalType == GoalType.WEIGHT_GAIN) {
                        opponentWeightChange = opponentCurrentWeight - opponentParticipant.getStartingWeight();
                        if (opponentParticipant.getTargetValue() != null && opponentParticipant.getTargetValue() > 0) {
                            opponentProgress = (opponentWeightChange / opponentParticipant.getTargetValue()) * 100;
                        }
                    } else if (opponentGoalType == GoalType.MAINTAIN) {
                        opponentWeightChange = opponentCurrentWeight - opponentParticipant.getStartingWeight();
                        opponentProgress = -Math.abs(opponentWeightChange);
                    }
                    // 기간을 계산합니다.
                    LocalDate now = LocalDate.now();
                    long totalDays = ChronoUnit.DAYS.between(battleRoom.getStartDate(), battleRoom.getEndDate());
                    long daysRemaining = ChronoUnit.DAYS.between(now, battleRoom.getEndDate());

                    // 승자를 결정 (진행률 기준으로)
                    String winner = "tie";
                    if (Math.round(myProgress) > Math.round(opponentProgress)) {
                        winner = "me";
                    } else if (Math.round(opponentProgress) > Math.round(myProgress)) {
                        winner = "opponent";
                    }

                    Double myTargetWeightLoss = null;
                    if (myGoalType == GoalType.WEIGHT_LOSS || myGoalType == GoalType.PERCENTAGE_LOSS) {
                        myTargetWeightLoss = calculateTargetWeightLoss(participant);
                    } else if (myGoalType == GoalType.WEIGHT_GAIN) {
                        myTargetWeightLoss = participant.getTargetValue();
                    } else if (myGoalType == GoalType.MAINTAIN) {
                        myTargetWeightLoss = 0.0;
                    }

                    Double opponentTargetWeightLoss = null;
                    if (opponentGoalType == GoalType.WEIGHT_LOSS || opponentGoalType == GoalType.PERCENTAGE_LOSS) {
                        opponentTargetWeightLoss = calculateTargetWeightLoss(opponentParticipant);
                    } else if (opponentGoalType == GoalType.WEIGHT_GAIN) {
                        opponentTargetWeightLoss = opponentParticipant.getTargetValue();
                    } else if (opponentGoalType == GoalType.MAINTAIN) {
                        opponentTargetWeightLoss = 0.0;
                    }

                    return BattleRoomDto.BattleSummaryData.builder()
                            .entryCode(battleRoom.getEntryCode())
                            .opponentNickname(opponentParticipant.getUser().getNickname())
                            .opponentUserId(opponentParticipant.getUser().getId())
                            .myStartWeight(participant.getStartingWeight())
                            .opponentStartWeight(opponentParticipant.getStartingWeight())
                            .myCurrentWeight(myCurrentWeight)
                            .opponentCurrentWeight(opponentCurrentWeight)
                            .myTargetWeightLoss(myTargetWeightLoss)
                            .opponentTargetWeightLoss(opponentTargetWeightLoss)
                            .startDate(battleRoom.getStartDate())
                            .endDate(battleRoom.getEndDate())
                            .status(battleRoom.getStatus().toString())
                            .myWeightLoss(myWeightChange)
                            .opponentWeightLoss(opponentWeightChange)
                            .myProgress(myProgress)
                            .opponentProgress(opponentProgress)
                            .totalDays((int) totalDays)
                            .daysRemaining((int) daysRemaining)
                            .winner(winner)
                            .roomName(battleRoom.getName())
                            .myGoalType(participant.getGoalType())
                            .opponentGoalType(opponentParticipant.getGoalType())
                            .build();
                })
                .collect(Collectors.toList());

        return BattleRoomDto.ActiveBattleData.builder()
                .activeBattles(battleSummaryData)
                .build();
    }

    public BattleRoomDto.ActiveBattleData getEndedBattles(String socialId) {
        UserB user1 = userRepository.findUserIdBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        /**
         * 배틀 목록을 조회하기 전, 만료된 배틀의 상태를 업데이트
         */
        checkAndEndBattles(user1);

        // 1. delYn이 'N'인 유효한 참가자만 가져옵니다.
        List<BattleParticipant> myParticipants = battleParticipantRepository.findAllByUserAndDelYnAndRoomDelYn(user1, 'N', 'N');

        List<BattleRoomDto.BattleSummaryData> battleSummaryData = myParticipants.stream()
                .filter(p -> p.getRoom().getStatus() == BattleStatus.ENDED)
                .map(participant -> {
                    BattleRoom battleRoom = participant.getRoom();
                    UserB user = participant.getUser();

                    // 2. 상대방 정보 조회
                    BattleParticipant opponentParticipant = battleParticipantRepository.findByRoomAndUserNotAndDelYn(battleRoom, user, 'N')
                            .orElseThrow(() -> new IllegalArgumentException("상대방이 없습니다."));

                    // 3. 최종 체중 정보 조회
                    // 배틀의 종료일(endDate)을 기준으로 최종 체중 기록을 가져옵니다.
                    Double myFinalWeight = weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(user, battleRoom.getEndDate().atStartOfDay())
                            .map(Weight::getWeight)
                            .orElse(participant.getStartingWeight());

                    Double opponentFinalWeight = weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(opponentParticipant.getUser(), battleRoom.getEndDate().atStartOfDay())
                            .map(Weight::getWeight)
                            .orElse(opponentParticipant.getStartingWeight());

                    // 목표 감량량을 계산합니다.
                    Double myTargetWeightLoss = calculateTargetWeightLoss(participant);
                    Double opponentTargetWeightLoss = calculateTargetWeightLoss(opponentParticipant);

                    // 최종 감량량 및 달성률 계산
                    Double myWeightLoss = myFinalWeight != null ? participant.getStartingWeight() - myFinalWeight : 0.0;
                    Double opponentWeightLoss = opponentFinalWeight != null ? opponentParticipant.getStartingWeight() - opponentFinalWeight : 0.0;

                    Double myProgress = (myTargetWeightLoss != null && myTargetWeightLoss > 0) ? (myWeightLoss / myTargetWeightLoss) * 100 : 0.0;
                    Double opponentProgress = (opponentTargetWeightLoss != null && opponentTargetWeightLoss > 0) ? (opponentWeightLoss / opponentTargetWeightLoss) * 100 : 0.0;

                    // 기간을 계산합니다.
                    long totalDays = ChronoUnit.DAYS.between(battleRoom.getStartDate(), battleRoom.getEndDate());

                    // 승자를 결정 (진행률 기준으로)
                    String winner = "tie";
                    if (myProgress > opponentProgress) {
                        winner = "me";
                    } else if (opponentProgress > myProgress) {
                        winner = "opponent";
                    }

                    return BattleRoomDto.BattleSummaryData.builder()
                            .entryCode(battleRoom.getEntryCode())
                            .opponentNickname(opponentParticipant.getUser().getNickname())
                            .opponentUserId(opponentParticipant.getUser().getId())
                            .myStartWeight(participant.getStartingWeight())
                            .opponentStartWeight(opponentParticipant.getStartingWeight())
                            // 종료된 배틀이므로 최종 체중을 현재 체중 필드에 매핑합니다.
                            .myCurrentWeight(myFinalWeight)
                            .opponentCurrentWeight(opponentFinalWeight)
                            .myTargetWeightLoss(myTargetWeightLoss)
                            .opponentTargetWeightLoss(opponentTargetWeightLoss)
                            .startDate(battleRoom.getStartDate())
                            .endDate(battleRoom.getEndDate())
                            .status(battleRoom.getStatus().toString())
                            .myWeightLoss(myWeightLoss)
                            .opponentWeightLoss(opponentWeightLoss)
                            .myProgress(myProgress)
                            .opponentProgress(opponentProgress)
                            .totalDays((int) totalDays)
                            .daysRemaining(0) // 종료된 배틀이므로 남은 기간은 0
                            .winner(winner)
                            .build();
                })
                .collect(Collectors.toList());

        return BattleRoomDto.ActiveBattleData.builder()
                .activeBattles(battleSummaryData)
                .build();
    }

    @Override
    public void deleteRoom(String entryCode, String socialId) {
        UserB user = userRepository.findBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        battleRoomRepository.deleteRoom(user.getId(), entryCode);

    }

    @Override
    public void leaveRoomByParticipant(String entryCode, String socialId) {
        UserB user = userRepository.findBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        battleParticipantRepository.leaveRoomByParticipant(user.getId(), entryCode);
    }

    @Override
    public List<BattleRoomDto.getParticipantsList> getParticipantsList(String entryCode) {

       return battleParticipantRepository.getParticipantsList(entryCode);
    }

    /**
     * 만료된 배틀의 상태를 업데이트하는 내부 로직
     */
    @Transactional
    private void checkAndEndBattles(UserB user) {
        LocalDate now = LocalDate.now();
        List<BattleParticipant> myParticipants = battleParticipantRepository.findAllByUserAndDelYnAndRoomDelYn(user, 'N', 'N');

        myParticipants.forEach(participant -> {
            BattleRoom room = participant.getRoom();
            if (room.getStatus() == BattleStatus.IN_PROGRESS && now.isAfter(room.getEndDate())) {
                room.setStatus(BattleStatus.ENDED);
                battleRoomRepository.save(room);
            }
        });
    }

    private Double calculateTargetWeightLoss(BattleParticipant participant) {
        if (participant.getGoalType() == GoalType.WEIGHT_LOSS) {
            return participant.getTargetValue();
        } else if (participant.getGoalType() == GoalType.PERCENTAGE_LOSS) {
            if (participant.getStartingWeight() != null && participant.getTargetValue() != null) {
                return participant.getStartingWeight() * (participant.getTargetValue() / 100.0);
            }
        }
        return 0.0;
    }
}
