package com.sa.baff.repository;

import com.sa.baff.model.dto.UserDto;

import java.util.List;

public interface UserFlagRepositoryCustom {
    List<UserDto.getUserFlagForPopUp> getUserFlagForPopUp(Long userId);
}
