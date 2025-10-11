package com.sa.baff.domain;

import com.sa.baff.common.AdminSyncStatus;
import com.sa.baff.domain.type.InquiryStatus;
import com.sa.baff.domain.type.InquiryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
@Table(name = "inquiry")
@Getter
public class Inquiry extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiryId")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private UserB user;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private InquiryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_type", nullable = false, length = 30)
    private InquiryType inquiryType;

    /**
     * 통합 어드민 연동 상태 (기본값: PENDING)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminSyncStatus adminSyncStatus;

    @Builder
    public Inquiry(String title, String content, UserB user, InquiryStatus status, InquiryType inquiryType) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.status = InquiryStatus.RECEIVED;
        this.inquiryType = inquiryType;
        this.adminSyncStatus = AdminSyncStatus.PENDING;
    }

    /**
     * 어드민 서버 연동 상태를 업데이트하는 메서드
     * @param adminSyncStatus 새로운 연동 상태
     */
    public void updateAdminSyncStatus(AdminSyncStatus adminSyncStatus) {
        this.adminSyncStatus = adminSyncStatus;
    }
}
