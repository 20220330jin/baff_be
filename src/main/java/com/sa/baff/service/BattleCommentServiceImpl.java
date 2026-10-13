package com.sa.baff.service;

import com.sa.baff.domain.BattleComment;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.BattleCommentDto;
import com.sa.baff.repository.BattleCommentRepository;
import com.sa.baff.repository.BattleParticipantRepository;
import com.sa.baff.repository.BattleRoomRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 대결 댓글 — 해당 대결방 참여자만 조회·작성·삭제 가능.
 */
@Service
@RequiredArgsConstructor
public class BattleCommentServiceImpl implements BattleCommentService {

    public static final int MAX_CONTENT_LENGTH = 200;

    private final BattleCommentRepository battleCommentRepository;
    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final AccountLinkedUserResolver accountLinkedUserResolver;

    @Override
    @Transactional(readOnly = true)
    public List<BattleCommentDto.commentItem> getComments(String entryCode, String socialId) {
        UserB user = resolveUser(socialId);
        BattleRoom room = resolveRoomAsParticipant(entryCode, user);

        return battleCommentRepository.findActiveByRoom(room).stream()
                .map(c -> toItem(c, user.getId()))
                .toList();
    }

    @Override
    @Transactional
    public BattleCommentDto.commentItem createComment(String entryCode, String content, String socialId) {
        UserB user = resolveUser(socialId);
        BattleRoom room = resolveRoomAsParticipant(entryCode, user);

        String trimmed = content == null ? "" : content.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("댓글은 " + MAX_CONTENT_LENGTH + "자까지 쓸 수 있어요.");
        }

        BattleComment saved = battleCommentRepository.save(BattleComment.builder()
                .room(room)
                .user(user)
                .content(trimmed)
                .build());
        return toItem(saved, user.getId());
    }

    @Override
    @Transactional
    public void deleteComment(String entryCode, Long commentId, String socialId) {
        UserB user = resolveUser(socialId);
        BattleRoom room = resolveRoomAsParticipant(entryCode, user);

        int updated = battleCommentRepository.softDeleteOwn(commentId, room, user.getId(), DateTimeUtils.now());
        if (updated == 0) {
            throw new IllegalArgumentException("삭제할 수 없는 댓글입니다.");
        }
    }

    private UserB resolveUser(String socialId) {
        return accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private BattleRoom resolveRoomAsParticipant(String entryCode, UserB user) {
        BattleRoom room = battleRoomRepository.findByEntryCode(entryCode)
                .filter(r -> r.getDelYn() == null || r.getDelYn() == 'N')
                .orElseThrow(() -> new EntityNotFoundException("대결방을 찾을 수 없습니다."));
        battleParticipantRepository.findByRoomAndUserAndDelYn(room, user, 'N')
                .orElseThrow(() -> new IllegalArgumentException("대결 참여자만 댓글을 볼 수 있어요."));
        return room;
    }

    private BattleCommentDto.commentItem toItem(BattleComment c, Long requesterId) {
        UserB author = c.getUser();
        return new BattleCommentDto.commentItem(
                c.getId(),
                author.getId(),
                author.getNickname(),
                c.getContent(),
                c.getRegDateTime(),
                author.getId().equals(requesterId));
    }
}
