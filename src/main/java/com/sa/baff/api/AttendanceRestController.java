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

    /** 오늘 출석 체크 */
    @PostMapping
    public AttendanceDto.checkResponse checkAttendance(@AuthenticationPrincipal String socialId) {
        return attendanceService.checkAttendance(socialId);
    }

    /** 출석 현황 조회 (달력, 연속일수, 다음 보너스) */
    @GetMapping("/status")
    public AttendanceDto.statusResponse getStatus(@AuthenticationPrincipal String socialId) {
        return attendanceService.getStatus(socialId);
    }
}
