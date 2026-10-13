package com.sa.baff.service;

import com.sa.baff.domain.BattleComment;
import com.sa.baff.domain.BattleParticipant;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.BattleCommentDto;
import com.sa.baff.repository.BattleCommentRepository;
import com.sa.baff.repository.BattleParticipantRepository;
import com.sa.baff.repository.BattleRoomRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleCommentServiceImplTest {

    @Mock private BattleCommentRepository battleCommentRepository;
    @Mock private BattleRoomRepository battleRoomRepository;
    @Mock private BattleParticipantRepository battleParticipantRepository;
    @Mock private AccountLinkedUserResolver accountLinkedUserResolver;

    @InjectMocks private BattleCommentServiceImpl service;

    private UserB me;
    private UserB opponent;
    private BattleRoom room;

    @BeforeEach
    void setUp() throws Exception {
        me = user(1L, "나");
        opponent = user(2L, "상대");
        room = new BattleRoom();
        room.setId(10L);
        room.setEntryCode("ABC123");
        when(accountLinkedUserResolver.resolveActiveUserBySocialId("me")).thenReturn(Optional.of(me));
        when(battleRoomRepository.findByEntryCode("ABC123")).thenReturn(Optional.of(room));
    }

    @Test
    @DisplayName("참여자가 아니면 댓글 조회 불가")
    void nonParticipantCannotRead() {
        when(battleParticipantRepository.findByRoomAndUserAndDelYn(room, me, 'N')).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getComments("ABC123", "me"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(battleCommentRepository, never()).findActiveByRoom(any());
    }

    @Test
    @DisplayName("목록은 본인 댓글에만 mine=true")
    void listMarksMine() throws Exception {
        participate();
        BattleComment mine = comment(100L, me, "화이팅");
        BattleComment theirs = comment(101L, opponent, "오늘 몇 kg?");
        when(battleCommentRepository.findActiveByRoom(room)).thenReturn(List.of(mine, theirs));

        List<BattleCommentDto.commentItem> result = service.getComments("ABC123", "me");

        assertThat(result).extracting(BattleCommentDto.commentItem::isMine).containsExactly(true, false);
        assertThat(result).extracting(BattleCommentDto.commentItem::getUserNickname).containsExactly("나", "상대");
    }

    @Test
    @DisplayName("작성 시 앞뒤 공백을 제거해 저장")
    void createTrims() {
        participate();
        when(battleCommentRepository.save(any(BattleComment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createComment("ABC123", "  오늘도 힘내요  ", "me");

        ArgumentCaptor<BattleComment> captor = ArgumentCaptor.forClass(BattleComment.class);
        verify(battleCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("오늘도 힘내요");
        assertThat(captor.getValue().getRoom()).isSameAs(room);
    }

    @Test
    @DisplayName("빈 댓글·200자 초과 거부")
    void createRejectsInvalid() {
        participate();

        assertThatThrownBy(() -> service.createComment("ABC123", "   ", "me"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createComment("ABC123", "가".repeat(201), "me"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(battleCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("본인 댓글이 아니면 삭제 실패")
    void deleteOthersFails() {
        participate();
        when(battleCommentRepository.softDeleteOwn(eq(101L), eq(room), eq(1L), any())).thenReturn(0);

        assertThatThrownBy(() -> service.deleteComment("ABC123", 101L, "me"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("본인 댓글 삭제 성공")
    void deleteOwn() {
        participate();
        when(battleCommentRepository.softDeleteOwn(eq(100L), eq(room), eq(1L), any())).thenReturn(1);

        service.deleteComment("ABC123", 100L, "me");

        verify(battleCommentRepository).softDeleteOwn(eq(100L), eq(room), anyLong(), any());
    }

    private void participate() {
        when(battleParticipantRepository.findByRoomAndUserAndDelYn(room, me, 'N'))
                .thenReturn(Optional.of(new BattleParticipant()));
    }

    private static UserB user(Long id, String nickname) throws Exception {
        UserB u = new UserB("e@x.com", nickname, null, "s" + id, "toss", "TOSS");
        setField(u, "id", id);
        return u;
    }

    private static BattleComment comment(Long id, UserB author, String content) throws Exception {
        BattleComment c = BattleComment.builder().user(author).content(content).build();
        setField(c, "id", id);
        return c;
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
