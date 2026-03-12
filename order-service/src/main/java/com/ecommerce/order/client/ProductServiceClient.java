package com.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "PRODUCT-SERVICE", fallback = ProductServiceFallback.class)
public interface ProductServiceClient {

    @PatchMapping("/products/{id}/stock/decrease")
    void decreaseStock(@PathVariable Long id, @RequestParam int quantity);
}
