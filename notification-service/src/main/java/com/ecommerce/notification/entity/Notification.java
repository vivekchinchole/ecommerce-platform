package com.ecommerce.notification.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    private Long userId;
    private Long orderId;
    private String type;     // ORDER_PLACED, ORDER_CANCELLED
    private String message;
    private NotificationStatus status;
    private LocalDateTime sentAt;

    public enum NotificationStatus {
        SENT, FAILED
    }
}
