package com.sa.baff.model.dto;

import com.sa.baff.domain.EditNicknameStatus;
import com.sa.baff.domain.Role;
import com.sa.baff.domain.UserB;
import lombok.*;

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

    public static UserDto from(UserB user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getProfileImageUrl(),
            user.getRole(),
            user.getHeight()
        );
    }

    @Setter
    @Getter
    public static class editNicknameStatus {
        private EditNicknameStatus status;
    }
}
