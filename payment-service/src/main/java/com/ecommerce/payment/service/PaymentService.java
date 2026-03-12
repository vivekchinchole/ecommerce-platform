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

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final IdempotencyService idempotencyService;
    private final RedissonClient redissonClient;
    private final PaymentEventPublisher publisher;

    public PaymentResponse createPayment(String key, PaymentRequest request) {

        PaymentResponse cached = idempotencyService.get(key);

        if (cached != null) return cached;

        RLock lock = redissonClient.getLock("payment-lock:"+key);

        try {
            lock.lock();

            Optional<Payment> existing =
                    repository.findByIdempotencyKey(key);

            if(existing.isPresent()){
                Payment p = existing.get();
                return new PaymentResponse(
                        p.getId(),
                        p.getStatus(),
                        p.getAmount(),
                        p.getCreatedAt()
                );
            }

            Payment payment = new Payment();

            payment.setIdempotencyKey(key);
            payment.setOrderId(request.getOrderId());
            payment.setUserId(request.getUserId());
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCreatedAt(LocalDateTime.now());

            repository.save(payment);

            PaymentResponse response =
                    new PaymentResponse(payment.getId(),
                            payment.getStatus(),
                            payment.getAmount(),
                            payment.getCreatedAt());

            idempotencyService.save(key,response);

            publisher.publish(payment);

            return response;

        } finally {
            lock.unlock();
        }
    }
}