package com.joaodev.minierp.product.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ProductResponse {
    private UUID id;
    private String name;
    private String sku;
    private BigDecimal price;
    private boolean active;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
