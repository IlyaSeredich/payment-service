package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.enumtype.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Page<Payment> findAllByUserId(UUID userId, Pageable pageable);

    Page<Payment> findAllByOrderId(Long orderId, Pageable pageable);

    Page<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

    List<Payment> findAllByUserIdAndTimestampBetween(UUID userId, LocalDateTime timestampAfter, LocalDateTime timestampBefore);

    List<Payment> findAllByTimestampBetween(LocalDateTime timestampAfter, LocalDateTime timestampBefore);
}
