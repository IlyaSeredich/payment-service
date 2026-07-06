package com.innowise.paymentservice.dto;

import java.time.LocalDateTime;

public record PaymentRangeDateDto(
        LocalDateTime from,
        LocalDateTime to
) {
}
