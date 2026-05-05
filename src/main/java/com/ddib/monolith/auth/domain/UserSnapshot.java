package com.ddib.monolith.auth.domain;

public record UserSnapshot(
        Long userId,
        String phoneNumber,
        String name,
        Role role
) {

    public static UserSnapshot from(User user) {
        return new UserSnapshot(
                user.getUserId(),
                user.getPhoneNumber(),
                user.getName(),
                user.getRole()
        );
    }
}

