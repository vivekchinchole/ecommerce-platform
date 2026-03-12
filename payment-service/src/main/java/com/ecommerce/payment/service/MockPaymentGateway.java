package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MockPaymentGateway {

    private final Random random = new Random();

    public boolean charge(Payment payment) {

        // simulate payment processing delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // simulate 80% success rate
        return random.nextInt(100) < 80;
    }
}