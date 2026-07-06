package com.innowise.paymentservice.entity;

import com.innowise.paymentservice.enumtype.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;
    private Long orderId;
    private UUID userId;
    private PaymentStatus status;
    private LocalDateTime timestamp;
    private BigDecimal paymentAmount;

}
