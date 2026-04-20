package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 계정 병합 감사 로그 (spec §3.1, §3.1.1).
 * 축소된 data_summary JSONB. 보관기간 180일 (retention_until).
 * 만료 후 배치가 data_summary=null + archived_at 세팅.
 */
@Entity
@Table(name = "account_merge_logs")
@Getter
@Setter
@NoArgsConstructor
public class AccountMergeLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "primary_user_id", nullable = false)
    private Long primaryUserId;

    @Column(name = "secondary_user_id", nullable = false)
    private Long secondaryUserId;

    @Column(name = "merged_at", nullable = false)
    private LocalDateTime mergedAt;

    /** 축소된 병합 요약 JSON. archive 이후 null. 본문성 데이터 저장 금지 (spec §3.1.1). */
    @Column(name = "data_summary", columnDefinition = "jsonb")
    private String dataSummary;

    @Column(name = "initiated_by", nullable = false, length = 20)
    private String initiatedBy;

    @Column(name = "retention_until", nullable = false)
    private LocalDateTime retentionUntil;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public AccountMergeLog(Long primaryUserId, Long secondaryUserId,
                           String dataSummary, String initiatedBy) {
        this.primaryUserId = primaryUserId;
        this.secondaryUserId = secondaryUserId;
        this.mergedAt = LocalDateTime.now();
        this.dataSummary = dataSummary;
        this.initiatedBy = initiatedBy;
        this.retentionUntil = this.mergedAt.plusDays(180);
    }

    public void archive() {
        this.dataSummary = null;
        this.archivedAt = LocalDateTime.now();
    }
}
