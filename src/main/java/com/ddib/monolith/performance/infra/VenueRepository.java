package com.ddib.monolith.performance.infra;

import com.ddib.monolith.performance.domain.Venue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    Optional<Venue> findByName(String name);
}

