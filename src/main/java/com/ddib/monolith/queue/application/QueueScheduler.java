package com.ddib.monolith.queue.application;

import com.ddib.monolith.queue.domain.QueueConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final QueueService queueService;

    @Scheduled(fixedRate = QueueConstants.SCHEDULER_RATE_MS)
    public void issueTokens() {
        queueService.schedule();
    }
}

