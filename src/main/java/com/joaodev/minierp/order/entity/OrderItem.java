package com.joaodev.minierp.order.entity;

import com.joaodev.minierp.common.entity.AuditableEntity;
import com.joaodev.minierp.product.entity.Product;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@SQLDelete(sql = "UPDATE order_item SET deleted_at = NOW(), updated_at = NOW(), version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
public class OrderItem extends AuditableEntity {
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Setter
    @Column(nullable = false)
    private Integer quantity;

    @Setter
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Setter
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
}
