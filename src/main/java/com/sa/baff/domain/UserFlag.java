package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "ux_user_flag_user_flagkey", columnNames = {"userId", "flagKey"})
})
@NoArgsConstructor
@Getter
@Setter
public class UserFlag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userFlagId")
    private Long id;

    @Column(name = "flagKey")
    private String flagKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private UserB user;

}
