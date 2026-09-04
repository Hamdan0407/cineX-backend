package com.bookmyshow.dto.mapper;

import com.bookmyshow.dto.PaymentDto;
import com.bookmyshow.entity.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentDto toResponse(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setBookingId(payment.getBookingId());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus());
        dto.setTransactionId(payment.getTransactionId());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}
