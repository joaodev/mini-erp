package com.joaodev.minierp.order.service;

import com.joaodev.minierp.common.exception.InvalidOrderStateException;
import com.joaodev.minierp.common.exception.ResourceNotFoundException;
import com.joaodev.minierp.customer.entity.Customer;
import com.joaodev.minierp.customer.repository.CustomerRepository;
import com.joaodev.minierp.order.dto.OrderItemRequest;
import com.joaodev.minierp.order.dto.OrderRequest;
import com.joaodev.minierp.order.dto.OrderResponse;
import com.joaodev.minierp.order.entity.Order;
import com.joaodev.minierp.order.entity.OrderItem;
import com.joaodev.minierp.order.entity.OrderStatus;
import com.joaodev.minierp.order.mapper.OrderMapper;
import com.joaodev.minierp.order.repository.OrderRepository;
import com.joaodev.minierp.product.entity.Product;
import com.joaodev.minierp.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository,
                        ProductRepository productRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id " + request.getCustomerId()));

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product  = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id " + itemRequest.getProductId()));

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(subtotal);
            order.addItem(item);

            total = total.add(subtotal);
        }
        order.setTotalAmount(total);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return orderMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toResponse);
    }

    @Transactional
    public OrderResponse confirm(UUID id) {
        Order order = getOrThrow(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only pending orders can be confirmed, current status is " + order.getStatus());
        }
        order.setStatus(OrderStatus.CONFIRMED);
        return orderMapper.toResponse(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderResponse cancel(UUID id) {
        Order order = getOrThrow(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order is already cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderMapper.toResponse(orderRepository.saveAndFlush(order));
    }




















    private Order getOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id" + id));
    }
}
