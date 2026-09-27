package com.joaodev.minierp.supplier.mapper;

import com.joaodev.minierp.common.mapper.PartyMapper;
import com.joaodev.minierp.supplier.dto.SupplierRequest;
import com.joaodev.minierp.supplier.dto.SupplierResponse;
import com.joaodev.minierp.supplier.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public Supplier toEntity(SupplierRequest request, String normalizedDocument) {
        Supplier supplier = new Supplier();
        supplier.setName(request.getName());
        supplier.setDocument(normalizedDocument);
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setActive(request.getActive() == null || request.getActive());
        return supplier;
    }

    public void updateEntity(Supplier supplier, SupplierRequest request, String normalizedDocument) {
        supplier.setName(request.getName());
        supplier.setDocument(normalizedDocument);
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        if (request.getActive() != null) {
            supplier.setActive(request.getActive());
        }
    }

    public SupplierResponse toResponse(Supplier supplier) {
        return PartyMapper.toResponse(supplier, SupplierResponse::new);
    }
}
