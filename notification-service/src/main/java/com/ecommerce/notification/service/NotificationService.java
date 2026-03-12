package com.ecommerce.notification.service;

import com.ecommerce.notification.config.OrderEvent;
import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void processOrderPlaced(OrderEvent event) {
        String message = String.format(
                "Your order #%d has been placed successfully! Total amount: $%.2f",
                event.getOrderId(), event.getTotalAmount());

        saveAndSendNotification(event, "ORDER_PLACED", message);
    }

    public void processOrderCancelled(OrderEvent event) {
        String message = String.format(
                "Your order #%d has been cancelled. Amount $%.2f will be refunded.",
                event.getOrderId(), event.getTotalAmount());

        saveAndSendNotification(event, "ORDER_CANCELLED", message);
    }

    private void saveAndSendNotification(OrderEvent event, String type, String message) {
        try {
            // Simulate email sending
            log.info("📧 Sending {} notification to userId={}: {}", type, event.getUserId(), message);

            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .orderId(event.getOrderId())
                    .type(type)
                    .message(message)
                    .status(Notification.NotificationStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
            log.info("Notification saved to MongoDB for orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process {} notification for orderId={}", type, event.getOrderId(), e);

            Notification failed = Notification.builder()
                    .userId(event.getUserId())
                    .orderId(event.getOrderId())
                    .type(type)
                    .message(message)
                    .status(Notification.NotificationStatus.FAILED)
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(failed);
        }
    }
}
