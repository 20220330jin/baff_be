package com.sa.baff.repository;

import com.sa.baff.domain.UserAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserAttendanceRepository extends JpaRepository<UserAttendance, Long> {

    Optional<UserAttendance> findByUserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    /** 해당 월의 출석 날짜 목록 */
    @Query("SELECT ua.attendanceDate FROM UserAttendance ua " +
           "WHERE ua.userId = :userId " +
           "AND ua.attendanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ua.attendanceDate ASC")
    List<LocalDate> findAttendanceDatesInMonth(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 특정 기간 내 출석 이력이 있는 사용자 ID 목록 */
    @Query("SELECT DISTINCT ua.userId FROM UserAttendance ua " +
           "WHERE ua.attendanceDate BETWEEN :startDate AND :endDate")
    List<Long> findUserIdsWithAttendanceBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 특정 날짜에 출석한 사용자 ID 목록 */
    @Query("SELECT ua.userId FROM UserAttendance ua WHERE ua.attendanceDate = :date")
    List<Long> findUserIdsByAttendanceDate(@Param("date") LocalDate date);
}
