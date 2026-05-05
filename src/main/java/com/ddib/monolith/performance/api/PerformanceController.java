package com.ddib.monolith.performance.api;

import com.ddib.monolith.performance.api.dto.PerformanceResponse;
import com.ddib.monolith.performance.api.dto.SeatRedisInfo;
import com.ddib.monolith.performance.application.PerformanceService;
import com.ddib.monolith.performance.domain.PerformanceCategory;
import com.ddib.monolith.performance.domain.PerformanceStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ticketing/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping("/{performanceId}")
    public ResponseEntity<PerformanceResponse> getPerformance(@PathVariable Long performanceId) {
        return ResponseEntity.ok(performanceService.getPerformance(performanceId));
    }

    @GetMapping
    public ResponseEntity<List<PerformanceResponse>> getAllPerformances(
            @RequestParam(required = false) PerformanceCategory category,
            @RequestParam(required = false) PerformanceStatus status
    ) {
        return ResponseEntity.ok(performanceService.getAllPerformances(category, status));
    }

    @GetMapping("/{performanceId}/options/{optionId}/seats")
    public ResponseEntity<SeatRedisInfo> getSeatInfo(@PathVariable Long performanceId, @PathVariable Long optionId) {
        return ResponseEntity.ok(performanceService.getSeatInfo(performanceId, optionId));
    }
}

