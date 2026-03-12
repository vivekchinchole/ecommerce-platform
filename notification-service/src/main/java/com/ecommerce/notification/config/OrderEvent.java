package com.ecommerce.notification.config;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {
    private Long orderId;
    private Long userId;
    private String eventType;
    private BigDecimal totalAmount;
    private LocalDateTime timestamp;
}
