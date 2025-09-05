package com.sa.baff.service;

import com.sa.baff.domain.BattleParticipant;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.BattleRoomDto;
import com.sa.baff.model.vo.BattleRoomVO;
import com.sa.baff.repository.BattleParticipantRepository;
import com.sa.baff.repository.BattleRoomRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.util.BattleStatus;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional

public class BattleServiceImpl implements BattleService {

    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final UserRepository userRepository;

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

        List<BattleParticipant> participants = battleParticipantRepository.findAllByUser(user);

        List<BattleRoomDto.getBattleRoomList> battleRoomList = participants.stream()
                .map(participant -> {
                    BattleRoom battleRoom = participant.getRoom();

                    BattleRoomDto.getBattleRoomList getBattleRoomList = new BattleRoomDto.getBattleRoomList();

                    getBattleRoomList.setName(battleRoom.getName());
                    getBattleRoomList.setPassword(battleRoom.getPassword());
                    getBattleRoomList.setDescription(battleRoom.getDescription());
                    getBattleRoomList.setHostId(battleRoom.getHost().getSocialId());
                    getBattleRoomList.setHostNickName(battleRoom.getHost().getNickname());
                    getBattleRoomList.setStatus(battleRoom.getStatus());
                    getBattleRoomList.setMaxParticipant(battleRoom.getMaxParticipants());
                    getBattleRoomList.setCurrentParticipant(battleRoom.getParticipants().size());
                    getBattleRoomList.setDurationDays(battleRoom.getDurationDays());
                    getBattleRoomList.setStartDate(battleRoom.getStartDate());
                    getBattleRoomList.setEndDate(battleRoom.getEndDate());
                    getBattleRoomList.setEntryCode(battleRoom.getEntryCode());

                    return getBattleRoomList;
                }).collect(Collectors.toList());

        return battleRoomList;
    }
}
