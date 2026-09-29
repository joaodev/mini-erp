package com.joaodev.minierp.order.controller;

import com.joaodev.minierp.order.dto.OrderRequest;
import com.joaodev.minierp.order.dto.OrderResponse;
import com.joaodev.minierp.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<OrderResponse> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirm(@PathVariable UUID id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
