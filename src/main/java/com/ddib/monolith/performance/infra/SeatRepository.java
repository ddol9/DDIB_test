package com.ddib.monolith.performance.infra;

import com.ddib.monolith.performance.domain.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findAllByVenueId(Long venueId);
}

