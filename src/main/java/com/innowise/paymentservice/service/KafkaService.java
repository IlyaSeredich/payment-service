package com.innowise.paymentservice.service;


public interface KafkaService {
    void sendMessage(Long orderId);
}
