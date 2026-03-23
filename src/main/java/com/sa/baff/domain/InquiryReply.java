package com.sa.baff.domain;

import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiry_replies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryReply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    public InquiryReply(Inquiry inquiry, String content, Long adminId) {
        super(DateTimeUtils.now(), DateTimeUtils.now());
        this.inquiry = inquiry;
        this.content = content;
        this.adminId = adminId;
    }
}
