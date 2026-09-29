package com.joaodev.minierp.order.service;

import com.joaodev.minierp.common.exception.InvalidOrderStateException;
import com.joaodev.minierp.common.exception.ResourceNotFoundException;
import com.joaodev.minierp.customer.entity.Customer;
import com.joaodev.minierp.customer.repository.CustomerRepository;
import com.joaodev.minierp.order.dto.OrderItemRequest;
import com.joaodev.minierp.order.dto.OrderRequest;
import com.joaodev.minierp.order.dto.OrderResponse;
import com.joaodev.minierp.order.entity.Order;
import com.joaodev.minierp.order.entity.OrderStatus;
import com.joaodev.minierp.order.mapper.OrderMapper;
import com.joaodev.minierp.order.repository.OrderRepository;
import com.joaodev.minierp.product.entity.Product;
import com.joaodev.minierp.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, customerRepository, productRepository, new OrderMapper());
    }

    @Test
    void shouldCreateOrderCalculatingSubtotalsAndTotal() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Customer customer = customer(customerId);
        Product product = product(productId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).then(returnsFirstArg());

        OrderResponse response = service.create(request(customerId, productId, 3));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("59.70"));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualTo(new BigDecimal("19.90"));
        assertThat(response.getItems().get(0).getSubtotal()).isEqualTo(new BigDecimal("59.70"));
    }

    @Test
    void shouldThrowNotFoundWhenCustomerDoesNotExist() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(customerId, UUID.randomUUID(), 1)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(customerId, productId, 1)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldFindOrderById() {
        UUID id = UUID.randomUUID();
        Order order = order(id, OrderStatus.PENDING);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        OrderResponse response = service.findById(id);

        assertThat(response.getId()).isEqualTo(id);
    }

    @Test
    void shouldThrowNotFoundWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldListOrdersWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(
                Collections.singletonList(order(UUID.randomUUID(), OrderStatus.PENDING)), pageable, 1);
        when(orderRepository.findAll(pageable)).thenReturn(page);

        Page<OrderResponse> result = service.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldConfirmPendingOrder() {
        UUID id = UUID.randomUUID();
        Order order = order(id, OrderStatus.PENDING);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any(Order.class))).then(returnsFirstArg());

        OrderResponse response = service.confirm(id);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldThrowInvalidStateWhenConfirmingNonPendingOrder() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order(id, OrderStatus.CONFIRMED)));

        assertThatThrownBy(() -> service.confirm(id))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void shouldThrowNotFoundWhenConfirmingMissingOrder() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldCancelPendingOrder() {
        UUID id = UUID.randomUUID();
        Order order = order(id, OrderStatus.PENDING);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any(Order.class))).then(returnsFirstArg());

        OrderResponse response = service.cancel(id);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldThrowInvalidStateWhenCancellingAlreadyCancelledOrder() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order(id, OrderStatus.CANCELLED)));

        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void shouldThrowNotFoundWhenCancellingMissingOrder() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private OrderRequest request(UUID customerId, UUID productId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);

        OrderRequest request = new OrderRequest();
        request.setCustomerId(customerId);
        request.setItems(Collections.singletonList(item));
        return request;
    }

    private Customer customer(UUID id) {
        Customer customer = new Customer();
        ReflectionTestUtils.setField(customer, "id", id);
        customer.setName("John Silva");
        customer.setDocument("52998224725");
        customer.setActive(true);
        return customer;
    }

    private Product product(UUID id) {
        Product product = new Product();
        ReflectionTestUtils.setField(product,"id", id);
        product.setName("Widget A");
        product.setSku("SKU-" + id);
        product.setPrice(new BigDecimal("19.90"));
        product.setActive(true);
        return product;
    }

    private Order order(UUID id, OrderStatus status) {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", id);
        order.setCustomer(customer(UUID.randomUUID()));
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("59.70"));
        return order;
    }
}
