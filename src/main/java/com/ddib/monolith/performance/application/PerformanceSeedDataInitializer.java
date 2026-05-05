package com.ddib.monolith.performance.application;

import com.ddib.monolith.performance.domain.Performance;
import com.ddib.monolith.performance.domain.PerformanceCategory;
import com.ddib.monolith.performance.domain.PerformanceOption;
import com.ddib.monolith.performance.domain.Seat;
import com.ddib.monolith.performance.domain.SeatTypePrice;
import com.ddib.monolith.performance.domain.Venue;
import com.ddib.monolith.performance.infra.PerformanceOptionRepository;
import com.ddib.monolith.performance.infra.PerformanceRepository;
import com.ddib.monolith.performance.infra.SeatRepository;
import com.ddib.monolith.performance.infra.SeatTypePriceRepository;
import com.ddib.monolith.performance.infra.VenueRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class PerformanceSeedDataInitializer implements ApplicationRunner {

    private final VenueRepository venueRepository;
    private final PerformanceRepository performanceRepository;
    private final PerformanceOptionRepository performanceOptionRepository;
    private final SeatTypePriceRepository seatTypePriceRepository;
    private final SeatRepository seatRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (performanceRepository.count() > 0) {
            return;
        }

        Venue venue = venueRepository.save(Venue.create("DDIB Arena", "Seoul", 12));
        Performance performance = performanceRepository.save(Performance.create(
                venue,
                PerformanceCategory.CONCERT,
                "DDIB Launch Concert",
                "Seed performance for monolith benchmark.",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                4,
                "/images/ddib-launch.png",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        ));

        performanceOptionRepository.save(new PerformanceOption(performance, LocalDateTime.now().plusDays(1).withHour(19).withMinute(0), 1, "OPEN"));
        performanceOptionRepository.save(new PerformanceOption(performance, LocalDateTime.now().plusDays(2).withHour(19).withMinute(0), 2, "OPEN"));

        seatTypePriceRepository.save(new SeatTypePrice(performance, "A", "VIP", 150000));
        seatTypePriceRepository.save(new SeatTypePrice(performance, "B", "R", 110000));

        for (int number = 1; number <= 4; number++) {
            seatRepository.save(Seat.create(venue.getId(), "A", number));
        }
        for (int number = 1; number <= 4; number++) {
            seatRepository.save(Seat.create(venue.getId(), "B", number));
        }
    }
}

