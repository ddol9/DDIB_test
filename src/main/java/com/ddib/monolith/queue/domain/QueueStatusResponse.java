package com.ddib.monolith.queue.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QueueStatusResponse {
    private Long rank;
    private String queueToken;
    private QueueRequestStatus status;
}

