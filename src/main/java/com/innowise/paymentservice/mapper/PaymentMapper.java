package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.PaymenResponseDto;
import com.innowise.paymentservice.dto.PaymentCreateDto;
import com.innowise.paymentservice.dto.SumResponseDto;
import com.innowise.paymentservice.entity.Payment;
import org.mapstruct.Mapper;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    Payment toPayment(PaymentCreateDto paymentCreateDto);
    PaymenResponseDto toDto(Payment payment);
    SumResponseDto toSumResponseDto(BigDecimal totalSum);
}
