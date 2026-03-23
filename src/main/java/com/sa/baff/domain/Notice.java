package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public Notice(String title, String content, Boolean isActive) {
        this.title = title;
        this.content = content;
        this.isActive = isActive != null ? isActive : true;
    }

    public void update(String title, String content, Boolean isActive) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (isActive != null) this.isActive = isActive;
    }
}
