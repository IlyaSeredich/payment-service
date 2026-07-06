package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.RandomNumFeignClient;
import com.innowise.paymentservice.dto.*;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.enumtype.PaymentStatus;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumFeignClient randomNumFeignClient;

    @Override
    public PaymenResponseDto createPayment(PaymentCreateDto paymentCreateDto) {
        Payment payment = paymentMapper.toPayment(paymentCreateDto);
        RandomNumResponseDto num = randomNumFeignClient.getNum();

        if(num.random() % 2 == 0) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        payment.setTimestamp(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toDto(savedPayment);
    }

    @Override
    public PagePaymentResponseDto getPaymentsByUserId(UUID userId, PageRequestDto pageRequestDto) {
        Pageable pageable = createPageable(pageRequestDto);
        Page<Payment> payments = paymentRepository.findAllByUserId(userId, pageable);
        return createPagePaymentResponseDto(payments);
    }

    @Override
    public PagePaymentResponseDto getPaymentsByOrderId(Long orderId, PageRequestDto pageRequestDto) {
        Pageable pageable = createPageable(pageRequestDto);
        Page<Payment> payments = paymentRepository.findAllByOrderId(orderId, pageable);
        return createPagePaymentResponseDto(payments);
    }

    @Override
    public PagePaymentResponseDto getPaymentsByStatus(PaymentStatus paymentStatus, PageRequestDto pageRequestDto) {
        Pageable pageable = createPageable(pageRequestDto);
        Page<Payment> payments = paymentRepository.findAllByStatus(paymentStatus, pageable);
        return createPagePaymentResponseDto(payments);
    }

    @Override
    public SumResponseDto getTotalSumByDateRange(
            UUID userId,
            PaymentRangeDateDto paymentRangeDateDto) {

        List<Payment> payments = paymentRepository.findAllByUserIdAndTimestampBetween(
                userId,
                paymentRangeDateDto.from(),
                paymentRangeDateDto.to()
        );

        BigDecimal totalSum = calculateTotalSum(payments);
        return paymentMapper.toSumResponseDto(totalSum);
    }

    @Override
    public SumResponseDto getTotalSumByDateRange(
            PaymentRangeDateDto paymentRangeDateDto) {

        List<Payment> payments = paymentRepository.findAllByTimestampBetween(
                paymentRangeDateDto.from(),
                paymentRangeDateDto.to()
        );

        BigDecimal totalSum = calculateTotalSum(payments);
        return paymentMapper.toSumResponseDto(totalSum);
    }



    private Pageable createPageable(PageRequestDto pageRequestDto) {
        return PageRequest.of(
                pageRequestDto.pageNumber(),
                pageRequestDto.pageSize(),
                Sort.by(
                        Sort.Direction.fromString(pageRequestDto.sortDirection()),
                        pageRequestDto.sortField()
                ));
    }

    private PagePaymentResponseDto createPagePaymentResponseDto(Page<Payment> searchedUsers) {
        return new PagePaymentResponseDto(
                searchedUsers.getContent().stream().map(paymentMapper::toDto).toList(),
                searchedUsers.getPageable().getPageNumber(),
                searchedUsers.getPageable().getPageSize(),
                searchedUsers.getTotalElements(),
                searchedUsers.getTotalPages()
        );
    }

    private BigDecimal calculateTotalSum(List<Payment> payments) {
        BigDecimal totalSum = new BigDecimal(0);

        for (Payment payment : payments) {
            totalSum = totalSum.add(payment.getPaymentAmount());
        }

        return totalSum;
    }

}
