package com.ecommerce.payment.kafka;

import com.ecommerce.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSuccess(Payment payment) {

        kafkaTemplate.send("payment.completed", payment);
    }

    public void publishFailed(Payment payment) {

        kafkaTemplate.send("payment.failed", payment);
    }
}