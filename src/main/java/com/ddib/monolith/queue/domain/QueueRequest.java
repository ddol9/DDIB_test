package com.ddib.monolith.queue.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QueueRequest(
        @NotNull @Positive Long performanceId,
        @NotNull @Positive Long optionId
) {
}

