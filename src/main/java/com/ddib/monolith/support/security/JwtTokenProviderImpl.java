package com.ddib.monolith.support.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String ACCESS = "ACCESS";

    private final JwtProperties properties;

    private SecretKey secretKey;

    @PostConstruct
    void initialize() {
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean validate(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public JwtPayload parse(String token) {
        Claims claims = parseClaims(token);
        return new JwtPayload(
                claims.getIssuer(),
                claims.getAudience() != null ? claims.getAudience().iterator().next() : null,
                claims.getExpiration().toInstant(),
                claims.getIssuedAt().toInstant(),
                claims.getId(),
                parseUserId(claims.getSubject()),
                claims.get(CLAIM_NAME, String.class),
                claims.get(CLAIM_ROLE, String.class),
                claims.get(CLAIM_TOKEN_TYPE, String.class)
        );
    }

    @Override
    public String generateAccessToken(Long userId, String name, String role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getAccessTokenValiditySeconds());

        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(userId))
                .audience().add(properties.getAudience()).and()
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_NAME, name)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_TYPE, ACCESS)
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .clockSkewSeconds(properties.getClockSkewSeconds())
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Long parseUserId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Long.parseLong(value);
    }
}

