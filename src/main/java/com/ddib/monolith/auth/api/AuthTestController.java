package com.ddib.monolith.auth.api;

import com.ddib.monolith.auth.api.dto.LoginResponse;
import com.ddib.monolith.auth.application.AuthTestService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/test")
@RequiredArgsConstructor
public class AuthTestController {

    private final AuthTestService authTestService;

    @PostMapping("/dummy-users")
    public ResponseEntity<List<Long>> createDummyUsers() {
        return ResponseEntity.ok(authTestService.createDummyUsers());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> testLogin(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(authTestService.loginTestUser(userId));
    }
}

