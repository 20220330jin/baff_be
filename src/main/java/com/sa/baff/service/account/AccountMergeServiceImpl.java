package com.sa.baff.service.account;

import com.sa.baff.domain.AccountLink;
import com.sa.baff.domain.AccountMergeLog;
import com.sa.baff.domain.Piece;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.UserFlag;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.domain.type.AttendanceSource;
import com.sa.baff.domain.type.UserStatus;
import com.sa.baff.repository.AccountLinkRepository;
import com.sa.baff.repository.AccountMergeLogRepository;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.UserFlagRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.util.InviteStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountMergeServiceImpl implements AccountMergeService {

    @PersistenceContext
    private EntityManager em;

    private final UserRepository userRepository;
    private final PieceRepository pieceRepository;
    private final AccountLinkRepository accountLinkRepository;
    private final AccountMergeLogRepository accountMergeLogRepository;
    private final UserFlagRepository userFlagRepository;

    private static final String PROVIDER_TOSS = "toss";
    private static final String INITIATED_BY_USER = "USER";
    private static final String FLAG_MERGE_COMPLETED = "account_merge_completed";

    @Override
    public Long merge(Long primaryUserId, Long secondaryUserId) {
        UserB primary = userRepository.findById(primaryUserId)
                .orElseThrow(() -> new IllegalStateException("PRIMARY_NOT_FOUND"));
        UserB secondary = userRepository.findById(secondaryUserId)
                .orElseThrow(() -> new IllegalStateException("SECONDARY_NOT_FOUND"));

        Piece primaryPiece = pieceRepository.findByUser(primary).orElse(null);
        Piece secondaryPiece = pieceRepository.findByUser(secondary).orElse(null);
        long pBefore = primaryPiece != null ? primaryPiece.getBalance() : 0L;
        long sBefore = secondaryPiece != null ? secondaryPiece.getBalance() : 0L;

        int weightCount = countQuery("SELECT COUNT(w) FROM Weight w WHERE w.user.id = :uid", secondaryUserId);
        int battleCount = countQuery("SELECT COUNT(bp) FROM BattleParticipant bp WHERE bp.user.id = :uid AND bp.delYn = 'N'", secondaryUserId);
        int attendanceCount = countQuery("SELECT COUNT(ua) FROM UserAttendance ua WHERE ua.userId = :uid", secondaryUserId);

        // 1. Piece 합산
        mergePiece(primary, primaryPiece, secondaryPiece, pBefore, sBefore);

        // 2. 단순 FK 재귀속 (ManyToOne user field)
        for (String entity : new String[]{
                "Weight", "Goals", "Inquiry", "Review", "ReviewComment",
                "FastingRecord", "RunningRecord", "BattleParticipant", "LoginHistory", "PieceTransaction"}) {
            em.createQuery("UPDATE " + entity + " e SET e.user = :p WHERE e.user = :s")
                    .setParameter("p", primary).setParameter("s", secondary).executeUpdate();
        }

        // 3. 단순 FK 재귀속 (Long userId field)
        for (String entity : new String[]{"RewardHistory", "ExchangeHistory", "AdWatchEvent", "SmartPushHistory"}) {
            em.createQuery("UPDATE " + entity + " e SET e.userId = :pId WHERE e.userId = :sId")
                    .setParameter("pId", primaryUserId).setParameter("sId", secondaryUserId).executeUpdate();
        }

        // 4. UserFlag 중복 제거 후 재귀속
        em.createQuery(
                "DELETE FROM UserFlag uf WHERE uf.user = :s " +
                "AND EXISTS (SELECT 1 FROM UserFlag uf2 WHERE uf2.user = :p AND uf2.flagKey = uf.flagKey)")
                .setParameter("s", secondary).setParameter("p", primary).executeUpdate();
        em.createQuery("UPDATE UserFlag uf SET uf.user = :p WHERE uf.user = :s")
                .setParameter("p", primary).setParameter("s", secondary).executeUpdate();

        // 5. UserAttendance 중복 제거 후 재귀속 (MERGED_TOSS)
        em.createNativeQuery(
                "DELETE FROM user_attendances WHERE user_id = :sId " +
                "AND attendance_date IN (SELECT attendance_date FROM user_attendances WHERE user_id = :pId)")
                .setParameter("sId", secondaryUserId).setParameter("pId", primaryUserId).executeUpdate();
        em.createQuery(
                "UPDATE UserAttendance ua SET ua.userId = :pId, ua.source = :src WHERE ua.userId = :sId")
                .setParameter("pId", primaryUserId).setParameter("sId", secondaryUserId)
                .setParameter("src", AttendanceSource.MERGED_TOSS).executeUpdate();

        // 6. ReviewLike 중복 제거 후 재귀속
        em.createQuery(
                "DELETE FROM ReviewLike rl WHERE rl.user = :s " +
                "AND EXISTS (SELECT 1 FROM ReviewLike rl2 WHERE rl2.review = rl.review AND rl2.user = :p)")
                .setParameter("s", secondary).setParameter("p", primary).executeUpdate();
        em.createQuery("UPDATE ReviewLike rl SET rl.user = :p WHERE rl.user = :s")
                .setParameter("p", primary).setParameter("s", secondary).executeUpdate();

        // 7. AiAnalysis 최신 유지 병합 (spec §3.3)
        // S3-15 P1-5: primary.analyzed_at IS NULL 케이스 방어 (null 비교는 null 반환 → UPDATE 미적용)
        em.createNativeQuery(
                "UPDATE ai_analysis p SET " +
                "  analyzed_at = s.analyzed_at, " +
                "  analysis_haiku = s.analysis_haiku, " +
                "  analysis_sonnet = s.analysis_sonnet, " +
                "  latest_record_at = s.latest_record_at " +
                "FROM ai_analysis s " +
                "WHERE p.user_id = :pId AND s.user_id = :sId " +
                "  AND p.feature_type = s.feature_type " +
                "  AND (p.analyzed_at IS NULL OR s.analyzed_at > p.analyzed_at)")
                .setParameter("pId", primaryUserId).setParameter("sId", secondaryUserId).executeUpdate();
        em.createQuery(
                "DELETE FROM AiAnalysis a WHERE a.userId = :sId " +
                "AND EXISTS (SELECT 1 FROM AiAnalysis a2 WHERE a2.userId = :pId AND a2.featureType = a.featureType)")
                .setParameter("sId", secondaryUserId).setParameter("pId", primaryUserId).executeUpdate();
        em.createQuery("UPDATE AiAnalysis a SET a.userId = :pId WHERE a.userId = :sId")
                .setParameter("pId", primaryUserId).setParameter("sId", secondaryUserId).executeUpdate();

        // 8. UserRewardDaily 합산 병합 (count / totalAmount)
        em.createNativeQuery(
                "UPDATE user_reward_dailies p SET " +
                "  count = p.count + s.count, " +
                "  total_amount = p.total_amount + s.total_amount " +
                "FROM user_reward_dailies s " +
                "WHERE p.user_id = :pId AND s.user_id = :sId " +
                "  AND p.reward_date = s.reward_date AND p.reward_type = s.reward_type")
                .setParameter("pId", primaryUserId).setParameter("sId", secondaryUserId).executeUpdate();
        em.createQuery(
                "DELETE FROM UserRewardDaily urd WHERE urd.userId = :sId " +
                "AND EXISTS (SELECT 1 FROM UserRewardDaily urd2 WHERE urd2.userId = :pId " +
                "AND urd2.rewardDate = urd.rewardDate AND urd2.rewardType = urd.rewardType)")
                .setParameter("sId", secondaryUserId).setParameter("pId", primaryUserId).executeUpdate();
        em.createQuery("UPDATE UserRewardDaily urd SET urd.userId = :pId WHERE urd.userId = :sId")
                .setParameter("pId", primaryUserId).setParameter("sId", secondaryUserId).executeUpdate();

        // 9. WeeklyMissionProgress 합산 병합 (currentCount, completed/rewardClaimed OR)
        em.createNativeQuery(
                "UPDATE weekly_mission_progress p SET " +
                "  current_count = p.current_count + s.current_count, " +
                "  completed = p.completed OR s.completed, " +
                "  reward_claimed = p.reward_claimed OR s.reward_claimed " +
                "FROM weekly_mission_progress s " +
                "WHERE p.user_id = :pId AND s.user_id = :sId " +
                "  AND p.week_start_date = s.week_start_date AND p.mission_type = s.mission_type")
                .setParameter("pId", primaryUserId).setParameter("sId", secondaryUserId).executeUpdate();
        em.createQuery(
                "DELETE FROM WeeklyMissionProgress wmp WHERE wmp.userId = :sId " +
                "AND EXISTS (SELECT 1 FROM WeeklyMissionProgress wmp2 WHERE wmp2.userId = :pId " +
                "AND wmp2.weekStartDate = wmp.weekStartDate AND wmp2.missionType = wmp.missionType)")
                .setParameter("sId", secondaryUserId).setParameter("pId", primaryUserId).executeUpdate();
        em.createQuery("UPDATE WeeklyMissionProgress wmp SET wmp.userId = :pId WHERE wmp.userId = :sId")
                .setParameter("pId", primaryUserId).setParameter("sId", secondaryUserId).executeUpdate();

        // 10. BattleInvite PENDING 취소 + 나머지 재귀속
        em.createQuery(
                "UPDATE BattleInvite i SET i.status = :newStatus WHERE i.inviter = :s AND i.status = :pending")
                .setParameter("newStatus", InviteStatus.CANCELLED_BY_MERGE)
                .setParameter("s", secondary).setParameter("pending", InviteStatus.PENDING).executeUpdate();
        em.createQuery(
                "UPDATE BattleInvite i SET i.status = :newStatus WHERE i.invitee = :s AND i.status = :pending")
                .setParameter("newStatus", InviteStatus.EXPIRED_BY_MERGE)
                .setParameter("s", secondary).setParameter("pending", InviteStatus.PENDING).executeUpdate();
        em.createQuery(
                "UPDATE BattleInvite i SET i.inviter = :p WHERE i.inviter = :s AND i.status != :pending")
                .setParameter("p", primary).setParameter("s", secondary)
                .setParameter("pending", InviteStatus.PENDING).executeUpdate();
        em.createQuery(
                "UPDATE BattleInvite i SET i.invitee = :p WHERE i.invitee = :s AND i.status != :pending")
                .setParameter("p", primary).setParameter("s", secondary)
                .setParameter("pending", InviteStatus.PENDING).executeUpdate();

        // 11. AccountLink 생성
        accountLinkRepository.save(new AccountLink(primaryUserId, PROVIDER_TOSS, secondary.getSocialId()));

        // 12. Secondary → MERGED
        secondary.setStatus(UserStatus.MERGED);
        secondary.setPrimaryUserId(primaryUserId);

        // 13. UserFlag account_merge_completed (Primary)
        UserFlag mergeFlag = new UserFlag();
        mergeFlag.setFlagKey(FLAG_MERGE_COMPLETED);
        mergeFlag.setUser(primary);
        userFlagRepository.save(mergeFlag);

        // 14. AccountMergeLog
        String dataSummary = String.format(
                "{\"pieces\":{\"added\":%d,\"total\":%d}," +
                "\"weightLogs\":%d,\"battles\":%d,\"attendances\":%d}",
                sBefore, pBefore + sBefore, weightCount, battleCount, attendanceCount);
        accountMergeLogRepository.save(
                new AccountMergeLog(primaryUserId, secondaryUserId, dataSummary, INITIATED_BY_USER));

        return primaryUserId;
    }

    private void mergePiece(UserB primary, Piece primaryPiece, Piece secondaryPiece,
                            long pBefore, long sBefore) {
        if (primaryPiece == null && secondaryPiece == null) return;

        if (primaryPiece == null) {
            secondaryPiece.setUser(primary);
        } else if (secondaryPiece != null) {
            primaryPiece.setBalance(pBefore + sBefore);
            primaryPiece.setTotalEarned(primaryPiece.getTotalEarned() + secondaryPiece.getTotalEarned());
            primaryPiece.setTotalExchanged(primaryPiece.getTotalExchanged() + secondaryPiece.getTotalExchanged());
            em.flush();
            pieceRepository.delete(secondaryPiece);
        }

        Piece finalPiece = primaryPiece != null ? primaryPiece : secondaryPiece;
        if (finalPiece.getBalance() != pBefore + sBefore) {
            throw new IllegalStateException("PIECE_INVARIANT_VIOLATION");
        }
    }

    private int countQuery(String jpql, Long userId) {
        Number n = (Number) em.createQuery(jpql).setParameter("uid", userId).getSingleResult();
        return n.intValue();
    }
}
