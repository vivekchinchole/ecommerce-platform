package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.kafka.PaymentEventPublisher;
import com.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final IdempotencyService idempotencyService;
    private final RedissonClient redissonClient;
    private final PaymentEventPublisher publisher;
    private final MockPaymentGateway mockGateway; // simulate gateway

    public PaymentResponse createPayment(String key, PaymentRequest request) throws InterruptedException {

        // 1️⃣ Check Redis idempotency cache
        PaymentResponse cached = idempotencyService.get(key);
        if (cached != null) {
            return cached;
        }

        // 2️⃣ Distributed lock
        RLock lock = redissonClient.getLock("payment-lock:" + key);

        boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

        if (!acquired) {
            throw new RuntimeException("Could not acquire payment lock");
        }

        try {

            // 3️⃣ Check DB for existing idempotency key
            Optional<Payment> existing = repository.findByIdempotencyKey(key);

            if (existing.isPresent()) {

                Payment p = existing.get();

                return new PaymentResponse(
                        p.getId(),
                        p.getStatus(),
                        p.getAmount(),
                        p.getCreatedAt()
                );
            }

            // 4️⃣ Create payment record
            Payment payment = new Payment();

            payment.setIdempotencyKey(key);
            payment.setOrderId(request.getOrderId());
            payment.setUserId(request.getUserId());
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setStatus(PaymentStatus.INITIATED);
            payment.setCreatedAt(LocalDateTime.now());

            repository.save(payment);

            // 5️⃣ Mark as PROCESSING
            payment.setStatus(PaymentStatus.PROCESSING);
            repository.save(payment);

            // 6️⃣ Call payment gateway (simulation)
            boolean success = mockGateway.charge(payment);

            if (success) {

                payment.setStatus(PaymentStatus.SUCCESS);
                repository.save(payment);

                publisher.publishSuccess(payment);

            } else {

                payment.setStatus(PaymentStatus.FAILED);
                repository.save(payment);

                publisher.publishFailed(payment);
            }

            // 7️⃣ Prepare response
            PaymentResponse response = new PaymentResponse(
                    payment.getId(),
                    payment.getStatus(),
                    payment.getAmount(),
                    payment.getCreatedAt()
            );

            // 8️⃣ Save idempotency response in Redis
            idempotencyService.save(key, response);

            return response;

        } finally {

            // 9️⃣ Safe unlock
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}