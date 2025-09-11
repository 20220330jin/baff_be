package com.sa.baff.domain;

import com.sa.baff.util.BattleStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "battle_rooms")
@Getter
@Setter
@NoArgsConstructor
public class BattleRoom extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String entryCode;

    private String name;
    private String password;
    private String description;
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    private BattleStatus status; // WAITING, IN_PROGRESS, ENDED

    @Column(nullable = false)
    private Integer durationDays; // 사용자가 선택한 기간 (7, 30, 60일 등)

    private LocalDate startDate; // 대결 시작일
    private LocalDate endDate; // 대결 종료일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private UserB host;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<BattleParticipant> participants = new ArrayList<>();

    @Builder
    public BattleRoom(String entryCode, String name, String password, String description, Integer maxParticipants, BattleStatus status, Integer durationDays, UserB host) {
        this.entryCode = entryCode;
        this.name = name;
        this.password = password;
        this.description = description;
        this.maxParticipants = maxParticipants;
        this.status = status;
        this.durationDays = durationDays;
        this.host = host;
    }
}