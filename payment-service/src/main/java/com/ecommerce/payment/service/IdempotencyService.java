package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String,Object> redisTemplate;

    public PaymentResponse get(String key) {
        return (PaymentResponse) redisTemplate.opsForValue().get(key);
    }

    public void save(String key, PaymentResponse response) {
        redisTemplate.opsForValue()
                .set(key, response, Duration.ofHours(24));
    }
}