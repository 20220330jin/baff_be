package com.sa.baff.service;

import com.sa.baff.model.dto.AttendanceDto;

public interface AttendanceService {

    /** 오늘 출석 체크 */
    AttendanceDto.checkResponse checkAttendance(String socialId, Boolean preAdWatched);

    /** 출석 현황 조회 (달력, 연속일수) */
    AttendanceDto.statusResponse getStatus(String socialId);
}
