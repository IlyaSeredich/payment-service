package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.PaymentCreateDto;
import com.innowise.paymentservice.dto.PaymentResponseDto;
import com.innowise.paymentservice.dto.SumResponseDto;
import com.innowise.paymentservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "userId", source = "userId")
    Payment toPayment(PaymentCreateDto paymentCreateDto, String userId);
    PaymentResponseDto toDto(Payment payment);
    SumResponseDto toSumResponseDto(BigDecimal totalSum);
}
