package com.sa.baff.service;

import com.sa.baff.model.dto.UserDto;

/**
 *
 * @author hjkim
 */
public interface UserService {
    UserDto getUserInfo(String userId);
}
