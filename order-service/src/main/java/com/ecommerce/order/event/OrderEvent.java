package com.ecommerce.order.event;

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
    private String eventType;  // ORDER_PLACED, ORDER_CANCELLED
    private BigDecimal totalAmount;
    private LocalDateTime timestamp;
}
