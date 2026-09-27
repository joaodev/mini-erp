package com.joaodev.minierp.customer.entity;

import com.joaodev.minierp.common.entity.Party;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer")
@SQLDelete(sql = "UPDATE customer SET deleted_at = NOW(), updated_at = NOW(), version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class Customer extends Party {
}
