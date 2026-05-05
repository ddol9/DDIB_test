package com.ddib.monolith.queue.application;

import com.ddib.monolith.queue.domain.QueueStatusResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class QueueEmitterRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long performanceId, Long optionId, Long userId) {
        String key = key(performanceId, optionId, userId);
        SseEmitter emitter = new SseEmitter(30_000L);
        emitters.put(key, emitter);
        emitter.onCompletion(() -> emitters.remove(key));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(key);
        });
        emitter.onError(ignored -> emitters.remove(key));
        return emitter;
    }

    public void push(Long performanceId, Long optionId, Long userId, QueueStatusResponse response, boolean complete) {
        SseEmitter emitter = emitters.get(key(performanceId, optionId, userId));
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("queue-status")
                    .data(response));
            if (complete) {
                emitter.complete();
                emitters.remove(key(performanceId, optionId, userId));
            }
        } catch (IOException exception) {
            emitter.completeWithError(exception);
            emitters.remove(key(performanceId, optionId, userId));
        }
    }

    private String key(Long performanceId, Long optionId, Long userId) {
        return performanceId + ":" + optionId + ":" + userId;
    }
}

