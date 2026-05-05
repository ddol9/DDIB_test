package com.ddib.monolith.support.security;

import com.ddib.monolith.support.exception.CommonErrorCode;
import com.ddib.monolith.support.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> WHITELIST_PATTERNS = List.of(
            "/api/auth/**",
            "/api/public/**",
            "/api/ticketing/performances",
            "/api/ticketing/performances/**",
            "/actuator/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error",
            "/favicon.ico"
    );

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (isWhitelisted(path)) {
            return true;
        }

        String token = jwtTokenProvider.resolveFromAuthorization(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (!StringUtils.hasText(token) || !jwtTokenProvider.validate(token)) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }

        JwtPayload payload = jwtTokenProvider.parse(token);
        if (payload.userId() == null) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }

        request.setAttribute(AuthAttributes.USER_ID, payload.userId());
        request.setAttribute(AuthAttributes.USER_NAME, payload.name());
        request.setAttribute(AuthAttributes.USER_ROLE, payload.role());
        return true;
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST_PATTERNS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
