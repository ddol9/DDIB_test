package com.ddib.monolith.seat.websocket.interceptor;

import com.ddib.monolith.queue.application.QueueService;
import com.ddib.monolith.queue.domain.QueueToken;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class TokenHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_PERFORMANCE_ID = "performanceId";
    public static final String ATTR_OPTION_ID = "optionId";
    public static final String ATTR_TOKEN_ID = "tokenId";

    private final QueueService queueService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        HttpServletRequest raw = servletRequest.getServletRequest();
        Long performanceId = parseLong(raw.getParameter("performanceId"));
        Long optionId = parseLong(raw.getParameter("optionId"));
        String tokenId = raw.getParameter("queueToken");
        if (performanceId == null || optionId == null || tokenId == null || tokenId.isBlank()) {
            return false;
        }
        Optional<QueueToken> token = queueService.findValidToken(performanceId, optionId, tokenId);
        if (token.isEmpty()) {
            return false;
        }

        attributes.put(ATTR_USER_ID, token.get().userId());
        attributes.put(ATTR_PERFORMANCE_ID, performanceId);
        attributes.put(ATTR_OPTION_ID, optionId);
        attributes.put(ATTR_TOKEN_ID, tokenId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }

    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

