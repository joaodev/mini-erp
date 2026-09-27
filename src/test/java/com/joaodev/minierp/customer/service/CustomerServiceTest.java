package com.joaodev.minierp.customer.service;

import com.joaodev.minierp.common.exception.DuplicateResourceException;
import com.joaodev.minierp.common.exception.ResourceNotFoundException;
import com.joaodev.minierp.customer.dto.CustomerRequest;
import com.joaodev.minierp.customer.dto.CustomerResponse;
import com.joaodev.minierp.customer.entity.Customer;
import com.joaodev.minierp.customer.mapper.CustomerMapper;
import com.joaodev.minierp.customer.repository.CustomerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    private static final String CPF = "52998224725";
    private static final String CNPJ = "11222333000181";

    @Mock
    private CustomerRepository repository;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(repository, new CustomerMapper());
    }

    @Test
    void shouldCreateCustomerWithNormalizedDocumentAndActiveByDefault() {
        when(repository.existsByDocument(CPF)).thenReturn(false);
        when(repository.save(any(Customer.class))).then(returnsFirstArg());

        CustomerResponse response = service.create(request("John Silva", "529.982.247-25"));

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDocument()).isEqualTo(CPF);
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(response.getName()).isEqualTo("John Silva");
        assertThat(response.getDocument()).isEqualTo(CPF);
    }

    @Test
    void shouldNotCreateCustomerWhenDocumentAlreadyExists() {
        when(repository.existsByDocument(CPF)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("John Silva", CPF)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any(Customer.class));
    }

    @Test
    void shouldFindCustomerById() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(customer(id, "John Silva", CPF)));

        CustomerResponse response = service.findById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("John Silva");
    }

    @Test
    void shouldThrowNotFoundWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldListCustomerWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(Arrays.asList(
                customer(UUID.randomUUID(), "Alice", CPF),
                customer(UUID.randomUUID(), "Bob", CNPJ)), pageable, 2);
        when(repository.findAll(pageable)).thenReturn(page);

        Page<CustomerResponse> result = service.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(CustomerResponse::getName)
                .containsExactly("Alice", "Bob");
    }

    @Test
    void shouldUpdateCustomerAndCheckNewDocument() {
        UUID id = UUID.randomUUID();
        Customer existing = customer(id, "Old Name", CPF);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsByDocument(CNPJ)).thenReturn(false);
        when(repository.saveAndFlush(any(Customer.class))).then(returnsFirstArg());

        CustomerResponse response = service.update(id, request("New Name", "11.222.333/0001-81"));

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getDocument()).isEqualTo(CNPJ);
    }

    @Test
    void shouldNotCheckDuplicateWhenDocumentDidNotChange() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(customer(id, "John Silva", CPF)));
        when(repository.saveAndFlush(any(Customer.class))).then(returnsFirstArg());

        service.update(id, request("John Updated", "529.982.247-25"));

        verify(repository, never()).existsByDocument(anyString());
    }

    @Test
    void shouldNotUpdateWhenNewDocumentBelongsToAnotherCustomer() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(customer(id, "John Silva", CPF)));
        when(repository.existsByDocument(CNPJ)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, request("John Silva", CNPJ)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).saveAndFlush(any(Customer.class));
    }

    @Test
    void shouldKeepActiveFlagWhenNotInformedOnUpdate() {
        UUID id = UUID.randomUUID();
        Customer existing = customer(id, "John Silva", CPF);
        existing.setActive(false);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(Customer.class))).then(returnsFirstArg());

        CustomerResponse response = service.update(id, request("John Silva", CPF));

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingCustomer() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request("John Silva", CPF)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteExistingCustomer() {
        UUID id = UUID.randomUUID();
        Customer existing = customer(id, "John Silva", CPF);
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(repository).delete(existing);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingMissingCustomer() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any(Customer.class));
    }

    private CustomerRequest request(String name, String document) {
        CustomerRequest request = new CustomerRequest();
        request.setName(name);
        request.setDocument(document);
        return request;
    }

    private Customer customer(UUID id, String name, String document) {
        Customer customer = new Customer();
        ReflectionTestUtils.setField(customer, "id", id);
        customer.setName(name);
        customer.setDocument(document);
        customer.setActive(true);
        return customer;
    }
}
