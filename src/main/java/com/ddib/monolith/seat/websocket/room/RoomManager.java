package com.ddib.monolith.seat.websocket.room;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RoomManager {

    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    public void joinRoom(Long performanceId, Long optionId, String sessionId) {
        sessionToRoom.put(sessionId, performanceId + ":" + optionId);
    }

    public void leaveRoom(String sessionId) {
        sessionToRoom.remove(sessionId);
    }
}

