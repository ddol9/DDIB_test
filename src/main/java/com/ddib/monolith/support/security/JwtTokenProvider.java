package com.ddib.monolith.support.security;

public interface JwtTokenProvider {

    boolean validate(String token);

    JwtPayload parse(String token);

    String generateAccessToken(Long userId, String name, String role);

    default String resolveFromAuthorization(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return authorizationHeader.substring(prefix.length()).trim();
        }
        return null;
    }
}

