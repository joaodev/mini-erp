package com.joaodev.minierp.supplier.repository;

import com.joaodev.minierp.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    Optional<Supplier> findByDocument(String document);
    boolean existsByDocument(String document);
}
