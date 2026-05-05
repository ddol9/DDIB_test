package com.ddib.monolith.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "performance_option", indexes = {
        @Index(name = "idx_performance_id", columnList = "performance_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "performance_option_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "option_order", nullable = false)
    private int optionOrder;

    @Column(name = "status", nullable = false)
    private String status;

    @Builder
    public PerformanceOption(Performance performance, LocalDateTime startAt, int optionOrder, String status) {
        this.performance = performance;
        this.startAt = startAt;
        this.optionOrder = optionOrder;
        this.status = status;
    }
}

