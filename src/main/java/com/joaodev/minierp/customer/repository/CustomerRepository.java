package com.joaodev.minierp.customer.repository;

import com.joaodev.minierp.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByDocument(String document);
    boolean existsByDocument(String document);
}
