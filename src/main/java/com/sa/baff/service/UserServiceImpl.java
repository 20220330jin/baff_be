package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto getUserInfo(String userId) {
        // userId는 JWT의 subject로, UserEntity의 socialId와 매핑됩니다.
        UserB user = userRepository.findUserIdBySocialId(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserDto.from(user);
    }
}
