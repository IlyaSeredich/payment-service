package com.innowise.paymentservice.config;

import lombok.AllArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@AllArgsConstructor
@Profile("test")
public class KafkaTestConfig {

    @Bean
    public NewTopic partitionTopic() {
        return new NewTopic("TestTopic",
                3,
                (short) 1);
    }
}