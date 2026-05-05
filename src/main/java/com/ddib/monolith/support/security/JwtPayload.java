package com.ddib.monolith.support.security;

import java.time.Instant;

public record JwtPayload(
        String issuer,
        String audience,
        Instant expiresAt,
        Instant issuedAt,
        String jti,
        Long userId,
        String name,
        String role,
        String tokenType
) {
}

