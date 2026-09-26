package com.joaodev.minierp.customer.service;

import com.joaodev.minierp.common.exception.DuplicateResourceException;
import com.joaodev.minierp.common.exception.ResourceNotFoundException;
import com.joaodev.minierp.common.util.DocumentUtils;
import com.joaodev.minierp.customer.dto.CustomerRequest;
import com.joaodev.minierp.customer.dto.CustomerResponse;
import com.joaodev.minierp.customer.entity.Customer;
import com.joaodev.minierp.customer.mapper.CustomerMapper;
import com.joaodev.minierp.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {
    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerService(CustomerRepository repository, CustomerMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String document = DocumentUtils.normalize(request.getDocument());
        if (repository.existsByDocument(document)) {
            throw new DuplicateResourceException("Customer already exists with document " + document);
        }
        Customer saved = repository.save(mapper.toEntity(request, document));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = getOrThrow(id);
        String document = DocumentUtils.normalize(request.getDocument());
        if (!customer.getDocument().equals(document) && repository.existsByDocument(document)) {
            throw new DuplicateResourceException("Customer already exists with document " + document);
        }
        mapper.updateEntity(customer, request, document);
        return mapper.toResponse(repository.saveAndFlush(customer));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(getOrThrow(id));
    }

    private Customer getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }
}
