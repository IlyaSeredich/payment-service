package com.innowise.paymentservice.dto;

import java.util.List;

public record PagePaymentResponseDto(
        List<PaymenResponseDto> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
){
}
