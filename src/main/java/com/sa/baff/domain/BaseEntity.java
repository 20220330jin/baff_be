package com.sa.baff.domain;

import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공통 ENTITY
 */
@NoArgsConstructor
@MappedSuperclass
@Getter
public abstract class BaseEntity {
    /** 등록시간 **/
    @Column(name = "regDateTime")
    private LocalDateTime regDateTime;
    /** 삭제여부 **/
    @Column(name = "delYn")
    private Character delYn = 'N';

    /** 마지막 변경시간 **/
    @Column(name = "modDateTime")
    private LocalDateTime modDateTime;


    public BaseEntity(LocalDateTime regDateTime, LocalDateTime modDateTime){
        this.regDateTime = regDateTime;
        this.modDateTime = modDateTime;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = DateTimeUtils.now();
        this.regDateTime = now;
        this.modDateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.modDateTime = DateTimeUtils.now();
    }
}

