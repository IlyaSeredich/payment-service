package com.innowise.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreateDto(
        @Min(value = 1, message = "order id must be more then 0")
        @NotNull(message = "order id must not be null")
        Long orderId,
        @DecimalMin(value = "0.01", message = "payment amount must be more then 0")
        @NotNull(message = "payment amount must not be null")
        BigDecimal paymentAmount
) {
}
