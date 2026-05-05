package com.ddib.monolith.payment.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class OrderIdGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public OrderIdGenerator(Clock clock) {
        this.clock = clock;
    }

    public String generate() {
        String date = LocalDate.now(clock).format(DATE_FORMATTER);
        return "ORDER-" + date + "-" + String.format("%06d", random.nextInt(1_000_000));
    }
}
