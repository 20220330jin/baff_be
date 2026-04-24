package com.sa.baff.api;

import com.sa.baff.model.dto.AttendanceDto;
import com.sa.baff.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reward/attendance")
@RequiredArgsConstructor
public class AttendanceRestController {

    private final AttendanceService attendanceService;

    /** 오늘 출석 체크 (S7-14: preAdWatched 제거) */
    @PostMapping
    public AttendanceDto.checkResponse checkAttendance(@AuthenticationPrincipal String socialId) {
        return attendanceService.checkAttendance(socialId);
    }

    /** 출석 현황 조회 (달력, 연속일수, canSaveStreak, 다음 보너스) */
    @GetMapping("/status")
    public AttendanceDto.statusResponse getStatus(@AuthenticationPrincipal String socialId) {
        return attendanceService.getStatus(socialId);
    }

    /** 출석 광고 보너스 지급 (S7-14 신규 경로) */
    @PostMapping("/ad-bonus")
    public AttendanceDto.adBonusResponse grantAdBonus(
            @AuthenticationPrincipal String socialId,
            @RequestBody(required = false) AttendanceDto.adBonusRequest request) {
        String platform = request != null ? request.getPlatform() : null;
        String adFormat = request != null ? request.getAdFormat() : null;
        return attendanceService.grantAdBonus(socialId, platform, adFormat);
    }

    /** 광고 시청으로 연속 출석 유지 (S7-14 신규) */
    @PostMapping("/streak-save")
    public AttendanceDto.streakSaveResponse saveStreak(
            @AuthenticationPrincipal String socialId,
            @RequestBody(required = false) AttendanceDto.adBonusRequest request) {
        String platform = request != null ? request.getPlatform() : null;
        String adFormat = request != null ? request.getAdFormat() : null;
        return attendanceService.saveStreak(socialId, platform, adFormat);
    }
}
