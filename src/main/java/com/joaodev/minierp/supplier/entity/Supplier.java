package com.joaodev.minierp.supplier.entity;

import com.joaodev.minierp.common.entity.Party;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplier")
@SQLDelete(sql = "UPDATE supplier SET deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Supplier extends Party {
}
