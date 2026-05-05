package com.ddib.monolith.performance.api.dto;

import com.ddib.monolith.performance.domain.Performance;
import com.ddib.monolith.performance.domain.PerformanceCategory;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PerformanceResponse {

    private Long performanceId;
    private Long venueId;
    private String venueName;
    private PerformanceCategory category;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxTicketsPerUser;
    private String performanceImg;
    private LocalDateTime bookingStartAt;
    private LocalDateTime bookingEndAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<PerformanceOptionResponse> options;

    public static PerformanceResponse from(Performance performance) {
        return PerformanceResponse.builder()
                .performanceId(performance.getId())
                .venueId(performance.getVenue().getId())
                .venueName(performance.getVenue().getName())
                .category(performance.getCategory())
                .title(performance.getTitle())
                .description(performance.getDescription())
                .startDate(performance.getStartDate())
                .endDate(performance.getEndDate())
                .maxTicketsPerUser(performance.getMaxTicketsPerUser())
                .performanceImg(performance.getPerformanceImg())
                .bookingStartAt(performance.getBookingStartAt())
                .bookingEndAt(performance.getBookingEndAt())
                .build();
    }

    public static PerformanceResponse from(Performance performance, List<PerformanceOptionResponse> options) {
        return PerformanceResponse.builder()
                .performanceId(performance.getId())
                .venueId(performance.getVenue().getId())
                .venueName(performance.getVenue().getName())
                .category(performance.getCategory())
                .title(performance.getTitle())
                .description(performance.getDescription())
                .startDate(performance.getStartDate())
                .endDate(performance.getEndDate())
                .maxTicketsPerUser(performance.getMaxTicketsPerUser())
                .performanceImg(performance.getPerformanceImg())
                .bookingStartAt(performance.getBookingStartAt())
                .bookingEndAt(performance.getBookingEndAt())
                .options(options)
                .build();
    }
}

