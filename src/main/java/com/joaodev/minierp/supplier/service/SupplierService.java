package com.joaodev.minierp.supplier.service;

import com.joaodev.minierp.common.exception.DuplicateResourceException;
import com.joaodev.minierp.common.exception.ResourceNotFoundException;
import com.joaodev.minierp.common.util.DocumentUtils;
import com.joaodev.minierp.supplier.dto.SupplierRequest;
import com.joaodev.minierp.supplier.dto.SupplierResponse;
import com.joaodev.minierp.supplier.entity.Supplier;
import com.joaodev.minierp.supplier.mapper.SupplierMapper;
import com.joaodev.minierp.supplier.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SupplierService {
    private final SupplierRepository repository;
    private final SupplierMapper mapper;

    public SupplierService(SupplierRepository repository, SupplierMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        String document = DocumentUtils.normalize(request.getDocument());
        if (repository.existsByDocument(document)) {
            throw new DuplicateResourceException("Supplier already exists with document " + document);
        }
        Supplier saved = repository.save(mapper.toEntity(request, document));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(UUID id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierRequest request) {
        Supplier supplier = getOrThrow(id);
        String document = DocumentUtils.normalize(request.getDocument());
        if (!supplier.getDocument().equals(document) && repository.existsByDocument(document)) {
            throw new DuplicateResourceException("Supplier already exists with document " + document);
        }
        mapper.updateEntity(supplier, request, document);
        return mapper.toResponse(repository.saveAndFlush(supplier));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(getOrThrow(id));
    }

    private Supplier getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + id));
    }
}
