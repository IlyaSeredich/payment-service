package com.innowise.paymentservice.dto;

import java.util.List;

public record PagePaymentResponseDto(
        List<PaymentResponseDto> content,
        Integer pageNumber,
        Integer pageSize,
        Long totalElements,
        Integer totalPages
){
}
