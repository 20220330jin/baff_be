package com.sa.baff.api;

import com.sa.baff.domain.BattleRoom;
import com.sa.baff.model.dto.BattleCommentDto;
import com.sa.baff.model.dto.BattleRoomDto;
import com.sa.baff.model.vo.BattleCommentVO;
import com.sa.baff.model.vo.BattleRoomVO;
import com.sa.baff.service.BattleCommentService;
import com.sa.baff.service.BattleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
   배틀 모드 관련 API 모음
*/

@RestController
@RequestMapping("/api/battle")
@RequiredArgsConstructor
public class BattleRestController {
    private final BattleService battleService;
    private final BattleCommentService battleCommentService;

    @PostMapping("/createBattleRoom")
    public void createBattleRoom(@RequestBody BattleRoomVO.createBattleRoom createBattleRoomParam, @AuthenticationPrincipal String socialId) {
        createBattleRoomParam.setSocialId(socialId);
        battleService.createBattleRoom(createBattleRoomParam);
    }

    @GetMapping("/getBattleRoomList")
    public List<BattleRoomDto.getBattleRoomList> getBattleRoomList(@AuthenticationPrincipal String socialId) {
        return battleService.getBattleRoomList(socialId);
    }

    @PostMapping("/{entryCode}/join")
    public void joinBattleRoom(@PathVariable String entryCode, @RequestBody BattleRoomVO.joinRequest joinRequest, @AuthenticationPrincipal String socialId) {
        battleService.joinBattleRoom(entryCode, joinRequest.getPassword(), socialId);
    }

    @GetMapping("/{entryCode}/details")
    public BattleRoomDto.getBattleRoomDetails.battleRoomDetail getBattleRoomDetails(@PathVariable String entryCode) {
        return battleService.getBattleRoomDetails(entryCode);
    }

    /** 대결 댓글 목록 (참여자 전용, 오래된 순) */
    @GetMapping("/{entryCode}/comments")
    public List<BattleCommentDto.commentItem> getBattleComments(@PathVariable String entryCode, @AuthenticationPrincipal String socialId) {
        return battleCommentService.getComments(entryCode, socialId);
    }

    /** 대결 댓글 작성 (참여자 전용, 최대 200자) */
    @PostMapping("/{entryCode}/comments")
    public BattleCommentDto.commentItem createBattleComment(@PathVariable String entryCode, @RequestBody BattleCommentVO.createComment param, @AuthenticationPrincipal String socialId) {
        return battleCommentService.createComment(entryCode, param.getContent(), socialId);
    }

    /** 대결 댓글 삭제 (본인 댓글만, 소프트 삭제) */
    @PostMapping("/{entryCode}/comments/{commentId}/delete")
    public void deleteBattleComment(@PathVariable String entryCode, @PathVariable Long commentId, @AuthenticationPrincipal String socialId) {
        battleCommentService.deleteComment(entryCode, commentId, socialId);
    }

    @PostMapping("/{entryCode}/battleGoalSetting")
    public void battleGoalSetting(@PathVariable String entryCode, @RequestBody BattleRoomVO.battleGoalSetting battleGoalSetting, @AuthenticationPrincipal String socialId) {
        battleService.battleGoalSetting(entryCode, battleGoalSetting, socialId);
    }

    @PostMapping("/{entryCode}/battleStart")
    public void battleStart(@PathVariable String entryCode, @AuthenticationPrincipal String socialId) {
        battleService.battleStart(entryCode, socialId);
    }

    @GetMapping("/active")
    public BattleRoomDto.ActiveBattleData activeBattles (@AuthenticationPrincipal String socialId) {
        return battleService.activeBattles(socialId);
    }
    @GetMapping("/getEndedBattles")
    public BattleRoomDto.ActiveBattleData getEndedBattles(@AuthenticationPrincipal String socialId) {
        return battleService.getEndedBattles(socialId);
    }

    @PostMapping("/{entryCode}/deleteRoom")
    public void deleteRoom(@PathVariable String entryCode, @AuthenticationPrincipal String socialId) {
        battleService.deleteRoom(entryCode, socialId);
    }

    @PostMapping("/{entryCode}/leaveRoomByParticipant")
    public void leaveRoomByParticipant(@PathVariable String entryCode, @AuthenticationPrincipal String socialId) {
        battleService.leaveRoomByParticipant(entryCode, socialId);
    }

    @GetMapping("/{entryCode}/getParticipantsList")
    public List<BattleRoomDto.getParticipantsList> getParticipantsList(@PathVariable String entryCode) {
        return battleService.getParticipantsList(entryCode);
    }

    @GetMapping("/{entryCode}/getBattleDetailForReview")
    public BattleRoomDto.getBattleDetailForReview getBattleDetailForReview(@PathVariable String entryCode, @AuthenticationPrincipal String socialId) {
        return battleService.getBattleDetailForReview(entryCode, socialId);
    }

    // === [토스] 초대 시스템 (토스 미니앱에서는 코드 참가 대신 초대 방식 사용) ===
    @PostMapping("/{entryCode}/invite")
    public void inviteUser(@PathVariable String entryCode, @RequestBody BattleRoomVO.inviteRequest request, @AuthenticationPrincipal String socialId) {
        battleService.inviteUser(entryCode, request.getInviteeUserId(), socialId);
    }

    @GetMapping("/invites")
    public List<BattleRoomDto.InviteInfo> getMyInvites(@AuthenticationPrincipal String socialId) {
        return battleService.getMyInvites(socialId);
    }

    @PostMapping("/invites/{inviteId}/accept")
    public void acceptInvite(@PathVariable Long inviteId, @AuthenticationPrincipal String socialId) {
        battleService.acceptInvite(inviteId, socialId);
    }

    @PostMapping("/invites/{inviteId}/decline")
    public void declineInvite(@PathVariable Long inviteId, @AuthenticationPrincipal String socialId) {
        battleService.declineInvite(inviteId, socialId);
    }

    @PostMapping("/{entryCode}/setBet")
    public void setBetAmount(@PathVariable String entryCode, @RequestBody BattleRoomVO.setBetRequest request, @AuthenticationPrincipal String socialId) {
        battleService.setBetAmount(entryCode, request.getBetAmount(), socialId);
    }

    // [토스] 초대 취소
    @PostMapping("/invites/{inviteId}/cancel")
    public void cancelInvite(@PathVariable Long inviteId, @AuthenticationPrincipal String socialId) {
        battleService.cancelInvite(inviteId, socialId);
    }

    // [토스] 보낸 초대 목록 조회
    @GetMapping("/{entryCode}/sentInvites")
    public List<BattleRoomDto.SentInviteInfo> getSentInvites(@PathVariable String entryCode, @AuthenticationPrincipal String socialId) {
        return battleService.getSentInvites(entryCode, socialId);
    }
}
