package com.ddib.monolith.queue.api;

import com.ddib.monolith.queue.application.QueueEmitterRegistry;
import com.ddib.monolith.queue.application.QueueService;
import com.ddib.monolith.queue.domain.QueueConstants;
import com.ddib.monolith.queue.domain.QueueRequest;
import com.ddib.monolith.queue.domain.QueueRequestStatus;
import com.ddib.monolith.queue.domain.QueueStatusResponse;
import com.ddib.monolith.support.security.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final QueueEmitterRegistry emitterRegistry;

    @PostMapping("/in")
    public boolean registerQueue(@RequestBody @Valid QueueRequest request, @UserId Long userId) {
        return queueService.registerWaitQueue(request.performanceId(), request.optionId(), userId);
    }

    @GetMapping(value = "/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getQueueStatus(@ModelAttribute QueueRequest request, @UserId Long userId) throws Exception {
        QueueStatusResponse current = queueService.registerEmitterAndGetCurrentStatus(
                request.performanceId(),
                request.optionId(),
                userId
        );
        SseEmitter emitter = emitterRegistry.register(request.performanceId(), request.optionId(), userId);
        emitter.send(SseEmitter.event().name(QueueConstants.SSE_EVENT_NAME).data(current));
        if (current.getStatus() != QueueRequestStatus.WAITING) {
            emitter.complete();
        }
        return emitter;
    }
}
