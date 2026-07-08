package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.*;
import com.innowise.paymentservice.enumtype.PaymentStatus;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@AllArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentCreateDto paymentCreateDto) {
        PaymentResponseDto paymentResponseDto = paymentService.createPayment(paymentCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponseDto);
    }

    @GetMapping("/my")
    public ResponseEntity<PagePaymentResponseDto> getUsersPayments(
            @ModelAttribute PageRequestDto pageRequestDto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        PagePaymentResponseDto payments =
                paymentService.getPaymentsByUserId(
                        UUID.fromString(jwt.getSubject()),
                        pageRequestDto);

        return ResponseEntity.ok(payments);
    }

    @GetMapping("/users")
    public ResponseEntity<PagePaymentResponseDto> getPaymentsByUserId(
            @Validated
            @RequestParam
            @org.hibernate.validator.constraints.UUID
            String userId,
            @ModelAttribute
            PageRequestDto pageRequestDto
    ) {
        PagePaymentResponseDto payments =
                paymentService.getPaymentsByUserId(UUID.fromString(userId), pageRequestDto);

        return ResponseEntity.ok(payments);
    }

    @GetMapping("/orders")
    public ResponseEntity<PagePaymentResponseDto> getPaymentsByOrderId(
            @Validated
            @RequestParam
            Long orderId,
            @ModelAttribute
            PageRequestDto pageRequestDto
    ) {
        PagePaymentResponseDto payments =
                paymentService.getPaymentsByOrderId(orderId, pageRequestDto);

        return ResponseEntity.ok(payments);
    }

    @GetMapping("/statuses")
    public ResponseEntity<PagePaymentResponseDto> getPaymentsByStatus(
            @Validated
            @RequestParam
            PaymentStatus status,
            @ModelAttribute
            PageRequestDto pageRequestDto
    ) {
        PagePaymentResponseDto payments =
                paymentService.getPaymentsByStatus(status, pageRequestDto);

        return ResponseEntity.ok(payments);
    }

    @GetMapping("/my/total")
    public ResponseEntity<SumResponseDto> getUsersSum(
            @RequestBody PaymentRangeDateDto paymentRangeDateDto,
            @AuthenticationPrincipal Jwt jwt) {

        SumResponseDto sumResponseDto =
                paymentService.getTotalSumByDateRange(
                        UUID.fromString(jwt.getSubject()),
                        paymentRangeDateDto);

        return ResponseEntity.ok(sumResponseDto);
    }

    @GetMapping("/total")
    public ResponseEntity<SumResponseDto> getAllSum(
            @RequestBody PaymentRangeDateDto paymentRangeDateDto) {

        SumResponseDto sumResponseDto =
                paymentService.getTotalSumByDateRange(
                        paymentRangeDateDto);

        return ResponseEntity.ok(sumResponseDto);
    }
}
