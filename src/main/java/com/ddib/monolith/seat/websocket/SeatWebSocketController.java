package com.ddib.monolith.seat.websocket;

import com.ddib.monolith.seat.application.BroadcastService;
import com.ddib.monolith.seat.application.SeatLockService;
import com.ddib.monolith.seat.application.SeatMessagingService;
import com.ddib.monolith.seat.domain.InitialStateMessage;
import com.ddib.monolith.seat.domain.SeatLockRequest;
import com.ddib.monolith.seat.domain.SeatLockResult;
import com.ddib.monolith.seat.domain.SeatReleaseRequest;
import com.ddib.monolith.seat.domain.SeatReleaseResult;
import com.ddib.monolith.seat.websocket.interceptor.TokenHandshakeInterceptor;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SeatWebSocketController {

    private final SeatLockService seatLockService;
    private final SeatMessagingService seatMessagingService;
    private final BroadcastService broadcastService;

    @MessageMapping("/seats/init")
    @SendToUser("/queue/seats")
    public InitialStateMessage init(SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        Long userId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_USER_ID);
        Long performanceId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_PERFORMANCE_ID);
        Long optionId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_OPTION_ID);
        String tokenId = (String) attrs.get(TokenHandshakeInterceptor.ATTR_TOKEN_ID);
        return seatLockService.getInitialState(userId, performanceId, optionId, tokenId);
    }

    @MessageMapping("/seats/lock")
    public void lock(SeatLockRequest request, SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        Long userId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_USER_ID);
        Long performanceId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_PERFORMANCE_ID);
        Long optionId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_OPTION_ID);
        String tokenId = (String) attrs.get(TokenHandshakeInterceptor.ATTR_TOKEN_ID);
        SeatLockResult result = seatLockService.lockSeats(userId, performanceId, optionId, tokenId, request.seatIds());
        seatMessagingService.publishLockResult(accessor.getSessionId(), result);
    }

    @MessageMapping("/seats/release")
    public void release(SeatReleaseRequest request, SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        Long userId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_USER_ID);
        Long performanceId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_PERFORMANCE_ID);
        Long optionId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_OPTION_ID);
        SeatReleaseResult result = seatLockService.releaseSeats(userId, performanceId, optionId, request.seatIds());
        seatMessagingService.publishReleaseResult(result);
    }

    @MessageMapping("/seats/going-to-payment")
    public void goingToPayment(SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        Long userId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_USER_ID);
        Long performanceId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_PERFORMANCE_ID);
        Long optionId = (Long) attrs.get(TokenHandshakeInterceptor.ATTR_OPTION_ID);
        seatLockService.setGoingToPayment(userId, performanceId, optionId);
    }
}

