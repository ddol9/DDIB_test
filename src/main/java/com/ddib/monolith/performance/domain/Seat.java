package com.ddib.monolith.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seat", uniqueConstraints = {
        @UniqueConstraint(name = "idx_venue_label_number", columnNames = {"venue_id", "seat_label", "seat_number"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long id;

    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    @Column(name = "seat_label", nullable = false)
    private String seatLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    public static Seat create(Long venueId, String seatLabel, Integer seatNumber) {
        return Seat.builder()
                .venueId(venueId)
                .seatLabel(seatLabel)
                .seatNumber(seatNumber)
                .build();
    }
}

