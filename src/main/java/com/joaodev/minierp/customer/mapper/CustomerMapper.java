package com.joaodev.minierp.customer.mapper;

import com.joaodev.minierp.common.mapper.PartyMapper;
import com.joaodev.minierp.customer.dto.CustomerRequest;
import com.joaodev.minierp.customer.dto.CustomerResponse;
import com.joaodev.minierp.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {
    public Customer toEntity(CustomerRequest request, String normalizedDocument) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setDocument(normalizedDocument);
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setActive(request.getActive() == null || request.getActive());
        return customer;
    }

    public void updateEntity(Customer customer, CustomerRequest request, String normalizedDocument) {
        customer.setName(request.getName());
        customer.setDocument(normalizedDocument);
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        if (request.getActive() != null) {
            customer.setActive(request.getActive());
        }
    }

    public CustomerResponse toResponse(Customer customer) {
        return PartyMapper.toResponse(customer, CustomerResponse::new);
    }
}
