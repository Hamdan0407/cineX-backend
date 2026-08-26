import os

base_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/src/main/java/com/bookmyshow'

files = {
    'entity/Payment.java': '''package com.bookmyshow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long bookingId;
    private Double amount;
    private String status;
    private String transactionId;
    private LocalDateTime createdAt = LocalDateTime.now();
}
''',
    'repository/PaymentRepository.java': '''package com.bookmyshow.repository;

import com.bookmyshow.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);
}
''',
    'dto/PaymentResponse.java': '''package com.bookmyshow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String status;
    private String transactionId;
}
''',
    'dto/PaymentDto.java': '''package com.bookmyshow.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentDto {
    private Long id;
    private Long bookingId;
    private Double amount;
    private String status;
    private String transactionId;
    private LocalDateTime createdAt;
}
''',
    'service/PaymentService.java': '''package com.bookmyshow.service;

import com.bookmyshow.dto.PaymentDto;
import com.bookmyshow.dto.PaymentResponse;
import com.bookmyshow.entity.Payment;
import com.bookmyshow.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse processPayment(Long bookingId, Double amount) {
        boolean isSuccess = Math.random() < 0.8;
        String status = isSuccess ? "SUCCESS" : "FAILURE";
        String transactionId = UUID.randomUUID().toString();

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setTransactionId(transactionId);
        paymentRepository.save(payment);

        return new PaymentResponse(status, transactionId);
    }

    public List<PaymentDto> getPaymentsByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream().map(payment -> {
            PaymentDto dto = new PaymentDto();
            dto.setId(payment.getId());
            dto.setBookingId(payment.getBookingId());
            dto.setAmount(payment.getAmount());
            dto.setStatus(payment.getStatus());
            dto.setTransactionId(payment.getTransactionId());
            dto.setCreatedAt(payment.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
}
''',
    'controller/PaymentController.java': '''package com.bookmyshow.controller;

import com.bookmyshow.dto.PaymentDto;
import com.bookmyshow.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentsByBookingId(bookingId));
    }
}
'''
}

for path, content in files.items():
    full_path = os.path.join(base_path, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w') as f:
        f.write(content)

print("Payment service generated successfully.")