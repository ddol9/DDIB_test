package com.ddib.monolith.auth.api.dto;

import com.ddib.monolith.auth.domain.Role;
import com.ddib.monolith.auth.domain.UserSnapshot;

public record UserResponse(
        Long userId,
        String nickname,
        Role role
) {

    public static UserResponse from(UserSnapshot user) {
        return new UserResponse(user.userId(), user.name(), user.role());
    }
}

