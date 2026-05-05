package com.ddib.monolith.performance.infra;

import com.ddib.monolith.performance.domain.PerformanceOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceOptionRepository extends JpaRepository<PerformanceOption, Long> {

    List<PerformanceOption> findAllByPerformanceIdOrderByStartAtAsc(Long performanceId);
}

