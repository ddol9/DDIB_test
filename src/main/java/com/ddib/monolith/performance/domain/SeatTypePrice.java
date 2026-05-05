package com.ddib.monolith.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seat_type_price")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatTypePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_type_price_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    private Performance performance;

    @Column(name = "seat_label", nullable = false)
    private String seatLabel;

    @Column(name = "seat_type", nullable = false)
    private String seatType;

    @Column(nullable = false)
    private int price;

    @Builder
    public SeatTypePrice(Performance performance, String seatLabel, String seatType, int price) {
        this.performance = performance;
        this.seatLabel = seatLabel;
        this.seatType = seatType;
        this.price = price;
    }
}

