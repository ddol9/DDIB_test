package com.ddib.monolith.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "performance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "performance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Enumerated(EnumType.STRING)
    private PerformanceCategory category;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(name = "max_tickets_per_user")
    private int maxTicketsPerUser;

    @Column(name = "performance_img")
    private String performanceImg;

    @Column(name = "booking_start_at", nullable = false)
    private LocalDateTime bookingStartAt;

    @Column(name = "booking_end_at", nullable = false)
    private LocalDateTime bookingEndAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Performance(
            Venue venue,
            PerformanceCategory category,
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            int maxTicketsPerUser,
            String performanceImg,
            LocalDateTime bookingStartAt,
            LocalDateTime bookingEndAt
    ) {
        this.venue = venue;
        this.category = category;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxTicketsPerUser = maxTicketsPerUser;
        this.performanceImg = performanceImg;
        this.bookingStartAt = bookingStartAt;
        this.bookingEndAt = bookingEndAt;
    }

    public static Performance create(
            Venue venue,
            PerformanceCategory category,
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            int maxTicketsPerUser,
            String performanceImg,
            LocalDateTime bookingStartAt,
            LocalDateTime bookingEndAt
    ) {
        return Performance.builder()
                .venue(venue)
                .category(category)
                .title(title)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .maxTicketsPerUser(maxTicketsPerUser)
                .performanceImg(performanceImg)
                .bookingStartAt(bookingStartAt)
                .bookingEndAt(bookingEndAt)
                .build();
    }
}

