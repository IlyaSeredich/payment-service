package com.innowise.paymentservice.config;

import com.innowise.paymentservice.config.properties.KafkaProperties;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class KafkaConfig {
    private final KafkaProperties kafkaProperties;

    @Bean
    public NewTopic emailSendingTopic() {
        return new NewTopic(kafkaProperties.getTopicName(),
                3,
                (short) 3);
    }
}
