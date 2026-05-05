package com.ddib.monolith.seat.application;

import com.ddib.monolith.seat.domain.SeatBroadcastMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastToRoom(Long performanceId, Long optionId, SeatBroadcastMessage message) {
        messagingTemplate.convertAndSend("/topic/seats." + performanceId + "." + optionId, message);
    }

    public void sendToUser(String sessionId, Object message) {
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/seats", message);
    }
}

