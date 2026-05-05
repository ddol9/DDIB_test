package com.ddib.monolith.performance.application;

import com.ddib.monolith.performance.api.dto.PerformanceOptionResponse;
import com.ddib.monolith.performance.api.dto.PerformanceResponse;
import com.ddib.monolith.performance.api.dto.SeatInfo;
import com.ddib.monolith.performance.api.dto.SeatRedisInfo;
import com.ddib.monolith.performance.api.dto.SeatTypeInfo;
import com.ddib.monolith.performance.domain.Performance;
import com.ddib.monolith.performance.domain.PerformanceCategory;
import com.ddib.monolith.performance.domain.PerformanceOption;
import com.ddib.monolith.performance.domain.PerformanceStatus;
import com.ddib.monolith.performance.domain.Seat;
import com.ddib.monolith.performance.domain.SeatTypePrice;
import com.ddib.monolith.performance.domain.Venue;
import com.ddib.monolith.performance.exception.PerformanceErrorCode;
import com.ddib.monolith.performance.infra.PerformanceOptionRepository;
import com.ddib.monolith.performance.infra.PerformanceRepository;
import com.ddib.monolith.performance.infra.SeatRepository;
import com.ddib.monolith.performance.infra.SeatTypePriceRepository;
import com.ddib.monolith.performance.infra.VenueRepository;
import com.ddib.monolith.support.exception.CustomException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceOptionRepository performanceOptionRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final SeatTypePriceRepository seatTypePriceRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PerformanceResponse getPerformance(Long performanceId) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new CustomException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        List<PerformanceOptionResponse> options = performanceOptionRepository
                .findAllByPerformanceIdOrderByStartAtAsc(performanceId)
                .stream()
                .map(PerformanceOptionResponse::from)
                .toList();

        return PerformanceResponse.from(performance, options);
    }

    public List<PerformanceResponse> getAllPerformances(PerformanceCategory category, PerformanceStatus status) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        List<Performance> performances;
        if (status == PerformanceStatus.BOOKING) {
            performances = category != null
                    ? performanceRepository.findAllByCategoryAndBookingStartAtBeforeAndBookingEndAtAfter(category, now, now)
                    : performanceRepository.findAllByBookingStartAtBeforeAndBookingEndAtAfter(now, now);
        } else if (status == PerformanceStatus.ENDED) {
            performances = category != null
                    ? performanceRepository.findAllByCategoryAndEndDateBefore(category, today)
                    : performanceRepository.findAllByEndDateBefore(today);
        } else {
            performances = category != null
                    ? performanceRepository.findAllByCategory(category)
                    : performanceRepository.findAll();
        }

        return performances.stream()
                .map(PerformanceResponse::from)
                .collect(Collectors.toList());
    }

    public SeatRedisInfo getSeatInfo(Long performanceId, Long optionId) {
        String key = seatInfoKey(optionId);

        try {
            String cachedValue = redisTemplate.opsForValue().get(key);
            if (cachedValue != null) {
                return objectMapper.readValue(cachedValue, SeatRedisInfo.class);
            }
        } catch (DataAccessException | JsonProcessingException exception) {
            log.debug("Seat cache read failed for optionId={}", optionId, exception);
        }

        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new CustomException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
        Venue venue = venueRepository.findById(performance.getVenue().getId())
                .orElseThrow(() -> new CustomException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        List<SeatTypePrice> prices = seatTypePriceRepository.findAllByPerformanceId(performanceId);
        List<Seat> seats = seatRepository.findAllByVenueId(venue.getId());
        SeatRedisInfo redisInfo = constructRedisInfo(prices, seats);

        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(redisInfo), 30, TimeUnit.MINUTES);
        } catch (DataAccessException | JsonProcessingException exception) {
            log.debug("Seat cache write failed for optionId={}", optionId, exception);
        }

        return redisInfo;
    }

    private SeatRedisInfo constructRedisInfo(List<SeatTypePrice> prices, List<Seat> seats) {
        Map<String, SeatTypeInfo> configMap = new HashMap<>();

        for (SeatTypePrice price : prices) {
            String key = price.getSeatType() + "_" + price.getPrice();
            configMap.computeIfAbsent(key, ignored -> new SeatTypeInfo(price.getSeatType(), price.getPrice(), new ArrayList<>(), 0));
            configMap.get(key).rows().add(price.getSeatLabel());
        }

        List<SeatTypeInfo> finalConfigs = new ArrayList<>();
        for (SeatTypeInfo config : configMap.values()) {
            int maxSeats = 0;
            for (String rowLabel : config.rows()) {
                long count = seats.stream().filter(seat -> seat.getSeatLabel().equals(rowLabel)).count();
                maxSeats = Math.max(maxSeats, (int) count);
            }
            finalConfigs.add(new SeatTypeInfo(config.seatType(), config.price(), config.rows(), maxSeats));
        }

        List<SeatInfo> seatInfos = seats.stream()
                .map(seat -> new SeatInfo(seat.getId(), seat.getSeatLabel(), seat.getSeatNumber()))
                .sorted(Comparator.comparing(SeatInfo::label).thenComparingInt(SeatInfo::number))
                .collect(Collectors.toList());

        return new SeatRedisInfo(finalConfigs, seatInfos);
    }

    private String seatInfoKey(Long optionId) {
        return "option:" + optionId + ":seats:info";
    }
}

