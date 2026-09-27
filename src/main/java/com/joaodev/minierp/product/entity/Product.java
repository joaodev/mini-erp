package com.joaodev.minierp.product.entity;

import com.joaodev.minierp.common.entity.AuditableEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
@SQLDelete(sql = "UPDATE product SET deleted_at = NOW(), updated_at = NOW(), version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Product extends AuditableEntity {
    @Setter
    @Column(nullable = false, length = 150)
    private String name;

    @Setter
    @EqualsAndHashCode.Include
    @Column(nullable = false, length = 50)
    private String sku;

    @Setter
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Setter
    @Column(nullable = false)
    private boolean active = true;
}
