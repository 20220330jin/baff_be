package com.sa.baff.domain;

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
    private InquiryStatus status = InquiryStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_type", nullable = false, length = 30)
    private InquiryType inquiryType;

    @Builder
    public Inquiry(String title, String content, UserB user, InquiryStatus status, InquiryType inquiryType) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.status = status;
        this.inquiryType = inquiryType;
    }
}
