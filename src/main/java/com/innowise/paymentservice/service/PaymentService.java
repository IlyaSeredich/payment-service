package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.*;
import com.innowise.paymentservice.enumtype.PaymentStatus;

import java.util.UUID;

public interface PaymentService {
    PaymentResponseDto createPayment(PaymentCreateDto paymentCreateDto, UUID id);
    PagePaymentResponseDto getPaymentsByUserId(UUID userId, PageRequestDto pageRequestDto);
    PagePaymentResponseDto getPaymentsByOrderId(Long orderId, PageRequestDto pageRequestDto);
    PagePaymentResponseDto getPaymentsByStatus(PaymentStatus paymentStatus, PageRequestDto pageRequestDto);
    SumResponseDto getTotalSumByDateRange(UUID userId, PaymentRangeDateDto paymentRangeDateDto);
    SumResponseDto getTotalSumByDateRange(PaymentRangeDateDto paymentRangeDateDto);
}
