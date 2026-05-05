package com.ddib.monolith.seat.websocket.interceptor;

import com.ddib.monolith.seat.websocket.room.RoomManager;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionTrackingInterceptor implements ChannelInterceptor {

    private final RoomManager roomManager;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null || accessor.getSessionId() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Long performanceId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_PERFORMANCE_ID);
            Long optionId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_OPTION_ID);
            if (performanceId != null && optionId != null) {
                roomManager.joinRoom(performanceId, optionId, accessor.getSessionId());
            }
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            roomManager.leaveRoom(accessor.getSessionId());
        }
        return message;
    }
}

