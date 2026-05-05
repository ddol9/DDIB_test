package com.ddib.monolith.auth.api;

import com.ddib.monolith.auth.api.dto.UserResponse;
import com.ddib.monolith.auth.api.dto.UserUpdateRequest;
import com.ddib.monolith.auth.application.UserService;
import com.ddib.monolith.support.security.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(@UserId Long userId) {
        return UserResponse.from(userService.getByUserId(userId));
    }

    @PatchMapping("/me")
    public UserResponse update(@UserId Long userId, @RequestBody UserUpdateRequest request) {
        return UserResponse.from(userService.updateUser(userId, request.nickname()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@UserId Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}

