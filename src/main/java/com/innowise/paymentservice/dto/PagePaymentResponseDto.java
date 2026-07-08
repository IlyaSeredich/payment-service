package com.innowise.paymentservice.dto;

import java.util.List;

public record PagePaymentResponseDto(
        List<PaymentResponseDto> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
){
}
