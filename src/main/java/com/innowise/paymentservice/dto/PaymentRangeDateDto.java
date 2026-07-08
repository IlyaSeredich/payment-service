package com.innowise.paymentservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PaymentRangeDateDto(
        @NotNull
        LocalDateTime from,
        @NotNull
        LocalDateTime to
) {
}
