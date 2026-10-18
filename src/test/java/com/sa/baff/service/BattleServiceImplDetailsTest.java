package com.sa.baff.service;

import com.sa.baff.domain.BattleParticipant;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.Weight;
import com.sa.baff.model.dto.BattleRoomDto;
import com.sa.baff.repository.BattleParticipantRepository;
import com.sa.baff.repository.BattleRoomRepository;
import com.sa.baff.repository.WeightRepository;
import com.sa.baff.util.BattleStatus;
import com.sa.baff.util.GoalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleServiceImplDetailsTest {

    @Mock private BattleRoomRepository battleRoomRepository;
    @Mock private BattleParticipantRepository battleParticipantRepository;
    @Mock private WeightRepository weightRepository;

    @InjectMocks private BattleServiceImpl service;

    private static final String ENTRY_CODE = "ABC123";

    @Test
    @DisplayName("종료된 대결은 종료일까지의 마지막 체중을 현재 체중으로 쓴다")
    void endedBattleUsesWeightUntilEndDate() throws Exception {
        UserB me = user(1L);
        LocalDate endDate = LocalDate.of(2026, 10, 10);
        BattleRoom room = room(me, BattleStatus.ENDED, endDate);
        BattleParticipant participant = participant(me, room);
        stubRoom(room, participant);

        when(weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(me, endDate.atTime(LocalTime.MAX)))
                .thenReturn(Optional.of(weight(75.0)));

        BattleRoomDto.getBattleRoomDetails.battleRoomDetail detail = service.getBattleRoomDetails(ENTRY_CODE);

        assertThat(detail.getParticipants().get(0).getCurrentWeight()).isEqualTo(75.0);
        verify(weightRepository, never()).findFirstByUserOrderByRecordDateDesc(any());
    }

    @Test
    @DisplayName("시작 전 대결(종료일 없음)은 최신 체중을 현재 체중으로 쓴다")
    void waitingBattleUsesLatestWeight() throws Exception {
        UserB me = user(1L);
        BattleRoom room = room(me, BattleStatus.WAITING, null);
        BattleParticipant participant = participant(me, room);
        stubRoom(room, participant);

        when(weightRepository.findFirstByUserOrderByRecordDateDesc(me)).thenReturn(Optional.of(weight(72.0)));

        BattleRoomDto.getBattleRoomDetails.battleRoomDetail detail = service.getBattleRoomDetails(ENTRY_CODE);

        assertThat(detail.getParticipants().get(0).getCurrentWeight()).isEqualTo(72.0);
    }

    private void stubRoom(BattleRoom room, BattleParticipant participant) {
        when(battleRoomRepository.findByEntryCode(ENTRY_CODE)).thenReturn(Optional.of(room));
        when(battleParticipantRepository.findAllByRoomAndDelYn(room, 'N')).thenReturn(List.of(participant));
    }

    private static BattleRoom room(UserB host, BattleStatus status, LocalDate endDate) {
        BattleRoom room = BattleRoom.builder()
                .entryCode(ENTRY_CODE)
                .status(status)
                .durationDays(7)
                .maxParticipants(2)
                .host(host)
                .build();
        if (endDate != null) {
            room.setStartDate(endDate.minusDays(7));
            room.setEndDate(endDate);
        }
        return room;
    }

    private static BattleParticipant participant(UserB user, BattleRoom room) {
        return BattleParticipant.builder()
                .user(user)
                .room(room)
                .startingWeight(80.0)
                .goalType(GoalType.WEIGHT_LOSS)
                .targetValue(5.0)
                .isReady(true)
                .build();
    }

    private static Weight weight(Double value) {
        return Weight.builder().weight(value).build();
    }

    private static UserB user(Long id) throws Exception {
        UserB u = new UserB("e@x.com", "나", null, "s" + id, "toss", "TOSS");
        setField(u, "id", id);
        return u;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
