package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.RandomNumFeignClient;
import com.innowise.paymentservice.dto.*;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.enumtype.PaymentStatus;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.KafkaService;
import com.innowise.paymentservice.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PaymentServiceImpl.class})
@ActiveProfiles("test")
class PaymentServiceImplTest {

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @MockitoBean
    private RandomNumFeignClient randomNumFeignClient;

    @MockitoBean
    private KafkaService kafkaService;

    @Autowired
    private PaymentService paymentService;

    private final UUID userId = UUID.randomUUID();
    private final Long orderId = 12345L;

    @AfterEach
    void cleanDb() {
        paymentRepository.deleteAll();
    }

    @Test
    void shouldCreateSuccessfulPaymentWhenRandomIsEven() {
        PaymentCreateDto createDto = new PaymentCreateDto(orderId, new BigDecimal(100));
        Payment payment = new Payment();
        PaymentResponseDto responseDto = new PaymentResponseDto(
                "1234",
                orderId,
                userId,
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                new BigDecimal(100));

        when(paymentMapper.toPayment(eq(createDto), any(String.class))).thenReturn(payment);
        when(randomNumFeignClient.getNum()).thenReturn(List.of(new RandomNumResponseDto(42L)));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(responseDto);

        PaymentResponseDto result = paymentService.createPayment(createDto, UUID.randomUUID());

        assertThat(result).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).save(payment);
    }

    @Test
    void shouldCreateFailedPaymentWhenRandomIsOdd() {
        PaymentCreateDto createDto = new PaymentCreateDto(
                orderId,
                new BigDecimal(100));

        Payment payment = new Payment();
        PaymentResponseDto responseDto = new PaymentResponseDto(
                "1234",
                orderId,
                userId,
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                new BigDecimal(100)
        );

        when(paymentMapper.toPayment(any(), any())).thenReturn(payment);
        when(randomNumFeignClient.getNum()).thenReturn(List.of(new RandomNumResponseDto(7L)));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(paymentMapper.toDto(any())).thenReturn(responseDto);

        paymentService.createPayment(createDto, UUID.randomUUID());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldGetPaymentsByUserId() {
        PageRequestDto pageDto = new PageRequestDto(
                0,
                10,
                "timestamp",
                "desc");
        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<Payment> page = new PageImpl<>(List.of(new Payment()), pageable, 1);
        PaymentResponseDto responseDto = new PaymentResponseDto(
                "1234",
                orderId,
                userId,
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                new BigDecimal(100));

        when(paymentRepository.findAllByUserId(userId.toString(), pageable)).thenReturn(page);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(responseDto);

        PagePaymentResponseDto result = paymentService.getPaymentsByUserId(userId, pageDto);

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void shouldGetPaymentsByOrderIdt() {
        PageRequestDto pageDto = new PageRequestDto(
                0, 10, "timestamp", "desc");
        Pageable pageable = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.DESC, "timestamp"));

        PaymentResponseDto responseDto = new PaymentResponseDto(
                "1234",
                orderId,
                userId,
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                new BigDecimal(100)
        );

        Page<Payment> page = new PageImpl<>(List.of(new Payment()), pageable, 1);

        when(paymentRepository.findAllByOrderId(orderId, pageable)).thenReturn(page);
        when(paymentMapper.toDto(any())).thenReturn(responseDto);

        PagePaymentResponseDto result = paymentService.getPaymentsByOrderId(orderId, pageDto);

        assertThat(result).isNotNull();
    }

    @Test
    void shouldGetPaymentsByStatus() {
        PageRequestDto pageDto = new PageRequestDto(
                0, 10, "timestamp", "desc");
        Pageable pageable = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.DESC, "timestamp"));

        PaymentResponseDto responseDto = new PaymentResponseDto(
                "1234",
                orderId,
                userId,
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                new BigDecimal(100)
        );

        Page<Payment> page = new PageImpl<>(List.of(new Payment()), pageable, 1);

        when(paymentRepository.findAllByStatus(PaymentStatus.SUCCESS, pageable)).thenReturn(page);
        when(paymentMapper.toDto(any())).thenReturn(responseDto);

        PagePaymentResponseDto result = paymentService.getPaymentsByStatus(PaymentStatus.SUCCESS, pageDto);

        assertThat(result).isNotNull();
    }

    @Test
    void shouldGetTotalSumByDateRangeWithUserId() {
        PaymentRangeDateDto dateDto = new PaymentRangeDateDto(
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        );

        List<Payment> payments = List.of(
                createPaymentWithAmount(new BigDecimal("100.50")),
                createPaymentWithAmount(new BigDecimal("200.00"))
        );

        when(paymentRepository.findAllByUserIdAndTimestampBetween(
                userId.toString(), dateDto.from(), dateDto.to()))
                .thenReturn(payments);

        when(paymentMapper.toSumResponseDto(any(BigDecimal.class)))
                .thenAnswer(inv -> new SumResponseDto(inv.getArgument(0)));

        SumResponseDto result = paymentService.getTotalSumByDateRange(userId, dateDto);

        assertThat(result.totalSum()).isEqualTo(new BigDecimal("300.50"));
    }

    @Test
    void shouldGetTotalSumByDateRangeWithoutUserId() {
        PaymentRangeDateDto dateDto = new PaymentRangeDateDto(
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        );

        List<Payment> payments = List.of(
                createPaymentWithAmount(new BigDecimal("50.00")),
                createPaymentWithAmount(new BigDecimal("50.25"))
        );

        when(paymentRepository.findAllByTimestampBetween(dateDto.from(), dateDto.to()))
                .thenReturn(payments);

        when(paymentMapper.toSumResponseDto(any(BigDecimal.class)))
                .thenAnswer(inv -> new SumResponseDto(inv.getArgument(0)));

        SumResponseDto result = paymentService.getTotalSumByDateRange(dateDto);

        assertThat(result.totalSum()).isEqualTo(new BigDecimal("100.25"));
    }

    private Payment createPaymentWithAmount(BigDecimal amount) {
        Payment payment = new Payment();
        payment.setPaymentAmount(amount);
        return payment;
    }
}