package com.joaodev.minierp.common.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public abstract class Party extends AuditableEntity {
    @Setter
    @EqualsAndHashCode.Include
    @Column(nullable = false, length = 150)
    private String name;

    @Setter
    @EqualsAndHashCode.Include
    @Column(nullable = false, length = 14)
    private String document;

    @Setter
    @Column(length = 150)
    private String email;

    @Setter
    @Column(length = 20)
    private String phone;

    @Setter
    @Column(nullable = false)
    private boolean active = true;
}
