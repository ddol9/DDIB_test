package com.ddib.monolith.support.web;

import com.ddib.monolith.support.security.UserId;
import com.ddib.monolith.support.security.UserName;
import com.ddib.monolith.support.security.UserRole;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DevProbeController {

    @GetMapping("/api/public/ping")
    public Map<String, Object> publicPing() {
        return Map.of(
                "scope", "public",
                "status", "ok"
        );
    }

    @GetMapping("/api/private/ping")
    public Map<String, Object> privatePing(
            @UserId Long userId,
            @UserName String userName,
            @UserRole String userRole
    ) {
        return Map.of(
                "scope", "private",
                "status", "ok",
                "userId", userId,
                "userName", userName,
                "userRole", userRole
        );
    }
}

