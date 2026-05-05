package com.ddib.monolith.auth.application;

import com.ddib.monolith.auth.domain.User;
import com.ddib.monolith.auth.domain.UserSnapshot;
import com.ddib.monolith.auth.exception.AuthErrorCode;
import com.ddib.monolith.auth.infra.UserRepository;
import com.ddib.monolith.support.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserSnapshot getByUserId(Long userId) {
        return userRepository.findById(userId)
                .map(UserSnapshot::from)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserSnapshot updateUser(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        user.updateName(nickname);
        return UserSnapshot.from(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }
}

