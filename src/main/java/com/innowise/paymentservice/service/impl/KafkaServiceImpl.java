package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.dto.PaymentResponseDto;
import com.innowise.paymentservice.service.KafkaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaServiceImpl implements KafkaService {
    @Value("${app.kafka.topics.email-sending}")
    private String topicName;
    private final KafkaTemplate<String, PaymentResponseDto> kafkaTemplate;

    @Override
    public void sendMessage(PaymentResponseDto paymentResponseDto) {
        kafkaTemplate.send(topicName, paymentResponseDto);
    }
}
