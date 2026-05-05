package com.ddib.monolith.performance.api.dto;

import com.ddib.monolith.performance.domain.PerformanceOption;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PerformanceOptionResponse {

    private Long performanceOptionId;
    private LocalDate startDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    private String status;

    public static PerformanceOptionResponse from(PerformanceOption option) {
        return PerformanceOptionResponse.builder()
                .performanceOptionId(option.getId())
                .startDate(option.getStartAt().toLocalDate())
                .startTime(option.getStartAt().toLocalTime().withSecond(0).withNano(0))
                .status(option.getStatus())
                .build();
    }
}

