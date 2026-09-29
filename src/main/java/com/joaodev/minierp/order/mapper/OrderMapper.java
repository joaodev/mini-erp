package com.joaodev.minierp.order.mapper;

import com.joaodev.minierp.order.dto.OrderItemResponse;
import com.joaodev.minierp.order.dto.OrderResponse;
import com.joaodev.minierp.order.entity.Order;
import com.joaodev.minierp.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomer().getId());
        response.setCustomerName(order.getCustomer().getName());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setVersion(order.getVersion());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setItems(order.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList()));
        return response;
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setProductName(item.getProduct().getName());
        response.setProductName(item.getProduct().getName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotal(item.getSubtotal());
        return response;
    }
}
