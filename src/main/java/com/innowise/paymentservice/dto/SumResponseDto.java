package com.innowise.paymentservice.dto;

import java.math.BigDecimal;

public record SumResponseDto(
        BigDecimal totalSum
) {
}
