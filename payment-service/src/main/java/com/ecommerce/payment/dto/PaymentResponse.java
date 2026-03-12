package com.ecommerce.payment.dto;


import com.ecommerce.payment.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private PaymentStatus status;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}