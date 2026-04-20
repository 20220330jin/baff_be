package com.sa.baff.domain;

import com.sa.baff.domain.type.AttendanceSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_attendances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "attendance_date"}))
@Getter
@Setter
@NoArgsConstructor
public class UserAttendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** 출석 시점의 연속일수 */
    @Column(nullable = false)
    private Integer streakCount;

    /** 광고로 유지된 가상 출석 여부 */
    @Column(nullable = false)
    private Boolean streakSaved = false;

    /** 출처 — 계정 통합 시 streak 계산에서 WEB 레거시 제외 (spec §3.2). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceSource source = AttendanceSource.WEB;

    public UserAttendance(Long userId, LocalDate attendanceDate, Integer streakCount, Boolean streakSaved) {
        this.userId = userId;
        this.attendanceDate = attendanceDate;
        this.streakCount = streakCount;
        this.streakSaved = streakSaved;
        this.source = AttendanceSource.WEB;
    }
}
