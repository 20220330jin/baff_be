package com.sa.baff.repository;

import com.sa.baff.domain.BattleComment;
import com.sa.baff.domain.BattleRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BattleCommentRepository extends JpaRepository<BattleComment, Long> {

    /** 대결방의 삭제되지 않은 댓글 (작성자 fetch join, 오래된 순). */
    @Query("SELECT c FROM BattleComment c JOIN FETCH c.user " +
           "WHERE c.room = :room AND c.delYn = 'N' ORDER BY c.regDateTime ASC, c.id ASC")
    List<BattleComment> findActiveByRoom(@Param("room") BattleRoom room);

    /** 본인 댓글만 소프트 삭제. 반영된 행 수 반환 (0 = 없거나 본인 댓글 아님). */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BattleComment c SET c.delYn = 'Y', c.modDateTime = :now " +
           "WHERE c.id = :commentId AND c.room = :room AND c.user.id = :userId AND c.delYn = 'N'")
    int softDeleteOwn(@Param("commentId") Long commentId,
                      @Param("room") BattleRoom room,
                      @Param("userId") Long userId,
                      @Param("now") LocalDateTime now);
}
