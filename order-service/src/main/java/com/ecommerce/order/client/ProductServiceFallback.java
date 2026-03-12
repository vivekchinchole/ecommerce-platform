package com.ecommerce.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductServiceFallback implements ProductServiceClient {

    @Override
    public void decreaseStock(Long id, int quantity) {
        log.error("Product service is unavailable. Could not decrease stock for product: {}", id);
        throw new RuntimeException("Product service unavailable. Please try again later.");
    }
}
