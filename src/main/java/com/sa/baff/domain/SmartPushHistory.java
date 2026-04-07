package com.sa.baff.domain;

import com.sa.baff.util.SmartPushType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "smart_push_histories")
@Getter
@Setter
@NoArgsConstructor
public class SmartPushHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SmartPushType pushType;

    /** 토스 API 응답 */
    @Column(columnDefinition = "TEXT")
    private String apiResponse;

    /** 성공 여부 */
    @Column(nullable = false)
    private Boolean success = false;

    public SmartPushHistory(Long userId, SmartPushType pushType, String apiResponse, Boolean success) {
        this.userId = userId;
        this.pushType = pushType;
        this.apiResponse = apiResponse;
        this.success = success;
    }
}
