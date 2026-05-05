package com.ddib.monolith.reservation.application;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReservationNoGenerator {

    public String generate() {
        return "RES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
