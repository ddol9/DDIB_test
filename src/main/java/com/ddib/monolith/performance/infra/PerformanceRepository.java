package com.ddib.monolith.performance.infra;

import com.ddib.monolith.performance.domain.Performance;
import com.ddib.monolith.performance.domain.PerformanceCategory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    Optional<Performance> findByTitle(String title);

    List<Performance> findAllByCategory(PerformanceCategory category);

    List<Performance> findAllByBookingStartAtBeforeAndBookingEndAtAfter(LocalDateTime now1, LocalDateTime now2);

    List<Performance> findAllByEndDateBefore(LocalDate date);

    List<Performance> findAllByCategoryAndBookingStartAtBeforeAndBookingEndAtAfter(
            PerformanceCategory category,
            LocalDateTime now1,
            LocalDateTime now2
    );

    List<Performance> findAllByCategoryAndEndDateBefore(PerformanceCategory category, LocalDate date);
}

