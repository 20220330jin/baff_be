package com.sa.baff.model.dto;

import com.sa.baff.domain.EditNicknameStatus;
import com.sa.baff.domain.Role;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.Gender;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String nickname;
    private String profileImage;
    private Role role;
    private Double height;
    private Gender gender;       // S6-30
    private LocalDate birthdate; // S6-30
    private LocalDateTime regDateTime;

    public static UserDto from(UserB user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getProfileImageUrl(),
            user.getRole(),
            user.getHeight(),
            user.getGender(),
            user.getBirthdate(),
            user.getRegDateTime()
        );
    }

    @Setter
    @Getter
    public static class editNicknameStatus {
        private EditNicknameStatus status;
    }

    @Getter
    @AllArgsConstructor
    public static class getUserFlagForPopUp {
        private Long userId;
        private String flagKey;
        private LocalDateTime regDateTime;
    }
}
