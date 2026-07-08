package com.innowise.paymentservice.config.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class KafkaProperties {
    @Value("${app.kafka.topics}")
    private String topicName;
}
