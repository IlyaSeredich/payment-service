package com.innowise.paymentservice.dto;

public record PageRequestDto(
        Integer pageNumber,
        Integer pageSize,
        String sortField,
        String sortDirection
) {
    public PageRequestDto {
        if (pageNumber == null || pageNumber < 0) pageNumber = 0;
        if (pageSize == null || pageSize <= 0) pageSize = 10;
        if (sortField == null || sortField.isBlank()) sortField = "timestamp";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "asc";
    }
}
