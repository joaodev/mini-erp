package com.joaodev.minierp.order.dto;

import com.joaodev.minierp.order.entity.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderResponse {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
