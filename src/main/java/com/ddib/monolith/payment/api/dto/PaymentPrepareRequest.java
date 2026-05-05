package com.ddib.monolith.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record PaymentPrepareRequest(
        @NotBlank String tokenId,
        @NotNull Long performanceId,
        @NotNull Long optionId,
        @NotNull @Positive Integer amount,
        @NotEmpty List<String> seatIds
) {
}
