package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.event.OrderEvent;
import com.ecommerce.order.exception.OrderCancellationException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    @Mock private ProductServiceClient productServiceClient;

    @InjectMocks private OrderService orderService;

    private Order pendingOrder;
    private Order confirmedOrder;

    @BeforeEach
    void setUp() {
        pendingOrder = Order.builder()
                .id(1L)
                .userId(10L)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .items(new ArrayList<>())
                .build();

        confirmedOrder = Order.builder()
                .id(2L)
                .userId(10L)
                .status(Order.OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("200.00"))
                .items(new ArrayList<>())
                .build();
    }

    @Test
    void createOrder_Success() {
        OrderDto.CreateRequest request = new OrderDto.CreateRequest();
        request.setUserId(10L);
        OrderDto.OrderItemRequest item = new OrderDto.OrderItemRequest(1L, 2, new BigDecimal("50.00"));
        request.setItems(List.of(item));

        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);
        when(kafkaTemplate.send(anyString(), anyString(), any(OrderEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        OrderDto.OrderResponse response = orderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(10L);
        verify(kafkaTemplate).send(eq("order.placed"), anyString(), any(OrderEvent.class));
    }

    @Test
    void cancelOrder_Success_WhenPending() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);
        when(kafkaTemplate.send(anyString(), anyString(), any(OrderEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThatNoException().isThrownBy(() -> orderService.cancelOrder(1L));
        verify(kafkaTemplate).send(eq("order.cancelled"), anyString(), any(OrderEvent.class));
    }

    @Test
    void cancelOrder_Throws_WhenNotPending() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(confirmedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(2L))
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void getOrderById_Throws_WhenNotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_ValidTransition() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        OrderDto.StatusUpdateRequest request = new OrderDto.StatusUpdateRequest(Order.OrderStatus.CONFIRMED);
        OrderDto.OrderResponse response = orderService.updateOrderStatus(1L, request);

        assertThat(response).isNotNull();
    }
}
