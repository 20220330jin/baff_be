package com.sa.baff.domain;

import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users") // 데이터베이스 테이블 이름을 'users'로 지정
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 보호
public class UserB extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId")
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // 이메일

    @Column(nullable = false)
    private String nickname; // 닉네임

    private String profileImageUrl; // 프로필 사진 URL

    @Column(unique = true, nullable = false)
    private String socialId; // 소셜 로그인 ID (예: "kakao_12345")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // 사용자 역할 (USER, ADMIN 등)

    @Column(nullable = true)
    private Double height;

    private String provider;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Goals> goals = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Weight> weights = new ArrayList<>();

    // 소셜 로그인 시 사용자 생성을 위한 생성자
    public UserB(String email, String nickname, String profileImageUrl, String socialId, String platform, Double height) {
        super(DateTimeUtils.now(), DateTimeUtils.now());
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.socialId = socialId;
        this.role = Role.USER; // 기본 역할은 USER로 설정
        this.provider = platform;
        this.height = height;
    }
}
