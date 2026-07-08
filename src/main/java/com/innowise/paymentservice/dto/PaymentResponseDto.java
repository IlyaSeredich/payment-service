package com.innowise.paymentservice.dto;

import com.innowise.paymentservice.enumtype.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponseDto(
        String id,
        Long orderId,
        UUID userId,
        PaymentStatus status,
        BigDecimal paymentAmount
) {
}
