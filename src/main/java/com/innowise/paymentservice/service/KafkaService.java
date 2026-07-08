package com.innowise.paymentservice.service;


import com.innowise.paymentservice.dto.PaymentResponseDto;

public interface KafkaService {
    void sendMessage(PaymentResponseDto paymentResponseDto);
}
