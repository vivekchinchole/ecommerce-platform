package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.event.OrderEvent;
import com.ecommerce.order.exception.InvalidOrderStatusException;
import com.ecommerce.order.exception.OrderCancellationException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private static final String ORDER_PLACED_TOPIC = "order.placed";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";

    // Valid status transitions
    private static final Map<Order.OrderStatus, Order.OrderStatus> VALID_TRANSITIONS = Map.of(
            Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED,
            Order.OrderStatus.CONFIRMED, Order.OrderStatus.SHIPPED,
            Order.OrderStatus.SHIPPED, Order.OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final ProductServiceClient productServiceClient;

    public OrderDto.OrderResponse createOrder(OrderDto.CreateRequest request) {
        BigDecimal total = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(request.getUserId())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(total)
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(req -> OrderItem.builder()
                        .order(order)
                        .productId(req.getProductId())
                        .quantity(req.getQuantity())
                        .price(req.getPrice())
                        .build())
                .toList();

        order.setItems(items);
        Order savedOrder = orderRepository.save(order);

        // Decrease stock via Feign client
        request.getItems().forEach(item ->
                productServiceClient.decreaseStock(item.getProductId(), item.getQuantity()));

        // Publish Kafka event
        publishOrderEvent(savedOrder, "ORDER_PLACED", ORDER_PLACED_TOPIC);

        log.info("Order created with id: {}", savedOrder.getId());
        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderResponse getOrderById(Long id) {
        Order order = findOrderById(id);
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto.OrderResponse> getOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    public OrderDto.OrderResponse updateOrderStatus(Long id, OrderDto.StatusUpdateRequest request) {
        Order order = findOrderById(id);
        Order.OrderStatus newStatus = request.getStatus();

        if (newStatus == Order.OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusException("Use DELETE endpoint to cancel orders");
        }

        Order.OrderStatus expectedNext = VALID_TRANSITIONS.get(order.getStatus());
        if (expectedNext == null || expectedNext != newStatus) {
            throw new InvalidOrderStatusException(
                    "Invalid status transition from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);
        return mapToResponse(orderRepository.save(order));
    }

    public void cancelOrder(Long id) {
        Order order = findOrderById(id);

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new OrderCancellationException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        publishOrderEvent(order, "ORDER_CANCELLED", ORDER_CANCELLED_TOPIC);
        log.info("Order cancelled: {}", id);
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private void publishOrderEvent(Order order, String eventType, String topic) {
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .eventType(eventType)
                .totalAmount(order.getTotalAmount())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(topic, String.valueOf(order.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} for order {}", eventType, order.getId(), ex);
                    } else {
                        log.info("Published event {} for order {}", eventType, order.getId());
                    }
                });
    }

    private OrderDto.OrderResponse mapToResponse(Order order) {
        List<OrderDto.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderDto.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .toList();

        return OrderDto.OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
