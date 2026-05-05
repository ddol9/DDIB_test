package com.ddib.monolith.performance.infra;

import com.ddib.monolith.performance.domain.SeatTypePrice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatTypePriceRepository extends JpaRepository<SeatTypePrice, Long> {

    List<SeatTypePrice> findAllByPerformanceId(Long performanceId);

    Optional<SeatTypePrice> findByPerformanceIdAndSeatLabel(Long performanceId, String seatLabel);
}

