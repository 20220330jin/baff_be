package com.sa.baff.service;

import com.sa.baff.domain.Role;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.vo.TossVO;
import com.sa.baff.provider.JwtProvider;
import com.sa.baff.repository.UserFlagRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plan v3 Task 1.5-2 — /api/toss/login 회귀 방어 단위 테스트.
 *
 * TossAuthService 추출 후 기존 loginWithToss 4개 경로가 그대로 동작하는지 Mockito로 검증.
 * Toss 실환경 의존 제거.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTossLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountLinkedUserResolver accountLinkedUserResolver;
    @Mock private UserFlagRepository userFlagRepository;
    @Mock private NicknameGeneratorService nicknameGeneratorService;
    @Mock private JwtProvider jwtProvider;
    @Mock private TossAuthService tossAuthService;

    @InjectMocks private UserServiceImpl userService;

    private static final String SOCIAL_ID = "12345";
    private static final String AUTH_CODE = "auth-code-xyz";
    private static final String REFERRER = "changeup";

    private TossVO.LoginRequest request;

    @BeforeEach
    void setUp() throws Exception {
        request = new TossVO.LoginRequest();
        setField(request, "authorizationCode", AUTH_CODE);
        setField(request, "referrer", REFERRER);
    }

    @Test
    @DisplayName("기존 유저 로그인 시 findBySocialId 결과로 JWT 발급")
    void loginWithToss_existingUser_returnsJwt() {
        UserB existing = new UserB("toss_12345@toss.im", "nickname", "img", SOCIAL_ID, "toss", "TOSS");
        setFieldQuietly(existing, "id", 1L);
        setBaseField(existing, "delYn", 'N');

        when(tossAuthService.resolveTossUserKey(AUTH_CODE, REFERRER))
                .thenReturn(new TossAuthService.TossUserKeyResult(SOCIAL_ID, 12345L, "email@toss.im", "이름"));
        when(userRepository.findBySocialId(SOCIAL_ID)).thenReturn(Optional.of(existing));
        when(jwtProvider.create(SOCIAL_ID, Role.USER.name())).thenReturn("jwt-token");

        String jwt = userService.loginWithToss(request, null);

        assertThat(jwt).isEqualTo("jwt-token");
        verify(userRepository, times(0)).reactivate(anyLong());
        verify(nicknameGeneratorService, times(0)).generateUniqueNicknameAndSave(any());
    }

    @Test
    @DisplayName("신규 유저 로그인 시 UserB 생성 + 닉네임/이미지 생성 + JWT 발급")
    void loginWithToss_newUser_createsUserAndReturnsJwt() {
        when(tossAuthService.resolveTossUserKey(AUTH_CODE, REFERRER))
                .thenReturn(new TossAuthService.TossUserKeyResult(SOCIAL_ID, 12345L, null, "이름"));
        when(userRepository.findBySocialId(SOCIAL_ID)).thenReturn(Optional.empty());
        when(nicknameGeneratorService.getRandomProfileImageUrl()).thenReturn("random.jpg");
        when(jwtProvider.create(eq(SOCIAL_ID), anyString())).thenReturn("jwt-new");

        String jwt = userService.loginWithToss(request, null);

        assertThat(jwt).isEqualTo("jwt-new");
        verify(nicknameGeneratorService, times(1)).generateUniqueNicknameAndSave(any(UserB.class));
    }

    @Test
    @DisplayName("delYn=Y 유저 재진입 시 reactivate 호출 후 JWT 발급")
    void loginWithToss_deletedUser_reactivates() {
        UserB deleted = new UserB("toss_12345@toss.im", "nick", "img", SOCIAL_ID, "toss", "TOSS");
        setFieldQuietly(deleted, "id", 42L);
        setBaseField(deleted, "delYn", 'Y');

        when(tossAuthService.resolveTossUserKey(AUTH_CODE, REFERRER))
                .thenReturn(new TossAuthService.TossUserKeyResult(SOCIAL_ID, 12345L, "e", "n"));
        when(userRepository.findBySocialId(SOCIAL_ID)).thenReturn(Optional.of(deleted));
        when(jwtProvider.create(SOCIAL_ID, Role.USER.name())).thenReturn("jwt-reactivated");

        String jwt = userService.loginWithToss(request, null);

        assertThat(jwt).isEqualTo("jwt-reactivated");
        verify(userRepository, times(1)).reactivate(42L);
    }

    @Test
    @DisplayName("TossAuthService 실패 시 예외가 그대로 전파된다 (매핑 경로 미변경 증명)")
    void loginWithToss_tossApiFailure_throwsMappedException() {
        when(tossAuthService.resolveTossUserKey(AUTH_CODE, REFERRER))
                .thenThrow(new IllegalStateException("Toss 연동이 설정되지 않았습니다."));

        assertThatThrownBy(() -> userService.loginWithToss(request, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Toss 연동");

        verify(userRepository, times(0)).findBySocialId(anyString());
        verify(jwtProvider, times(0)).create(anyString(), anyString());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void setFieldQuietly(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setBaseField(Object target, String name, Object value) {
        try {
            Class<?> c = target.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    c = c.getSuperclass();
                }
            }
            throw new RuntimeException("field not found: " + name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
