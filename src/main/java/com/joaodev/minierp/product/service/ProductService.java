package com.joaodev.minierp.product.service;

import com.joaodev.minierp.common.exception.DuplicateResourceException;
import com.joaodev.minierp.common.exception.ResourceNotFoundException;
import com.joaodev.minierp.product.dto.ProductRequest;
import com.joaodev.minierp.product.dto.ProductResponse;
import com.joaodev.minierp.product.entity.Product;
import com.joaodev.minierp.product.mapper.ProductMapper;
import com.joaodev.minierp.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final ProductMapper mapper;

    public ProductService(ProductRepository repository, ProductMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = normalize(request.getSku());
        if (repository.existsBySku(sku)) {
            throw new DuplicateResourceException("Product already exists with SKU " + sku);
        }
        Product saved = repository.save(mapper.toEntity(request, sku));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public Page<ProductResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = getOrThrow(id);
        String sku = normalize(request.getSku());
        if (!product.getSku().equals(sku) && repository.existsBySku(sku)) {
            throw new DuplicateResourceException("Product already exists with SKU " + sku);
        }
        mapper.updateEntity(product, request, sku);
        return mapper.toResponse(repository.saveAndFlush(product));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(getOrThrow(id));
    }

    private Product getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    private String normalize(String sku) {
        return sku == null ? null : sku.trim().toUpperCase();
    }
}
