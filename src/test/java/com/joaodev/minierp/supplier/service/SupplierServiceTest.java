package com.joaodev.minierp.supplier.service;

import com.joaodev.minierp.common.exception.DuplicateResourceException;
import com.joaodev.minierp.common.exception.ResourceNotFoundException;
import com.joaodev.minierp.supplier.dto.SupplierRequest;
import com.joaodev.minierp.supplier.dto.SupplierResponse;
import com.joaodev.minierp.supplier.entity.Supplier;
import com.joaodev.minierp.supplier.mapper.SupplierMapper;
import com.joaodev.minierp.supplier.repository.SupplierRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SupplierServiceTest {

    private static final String CPF = "52998224725";
    private static final String CNPJ = "11222333000181";

    @Mock
    private SupplierRepository repository;

    private SupplierService service;

    @BeforeEach
    void setUp() {
        service = new SupplierService(repository, new SupplierMapper());
    }

    @Test
    void shouldCreateSupplierWithNormalizedDocumentAndActiveByDefault() {
        when(repository.existsByDocument(CNPJ)).thenReturn(false);
        when(repository.save(any(Supplier.class))).then(returnsFirstArg());

        SupplierResponse response = service.create(request("Global Parts Ltd", "11.222.333/0001-81"));

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDocument()).isEqualTo(CNPJ);
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(response.getName()).isEqualTo("Global Parts Ltd");
        assertThat(response.getDocument()).isEqualTo(CNPJ);
    }

    @Test
    void shouldNotCreateSupplierWhenDocumentAlreadyExists() {
        when(repository.existsByDocument(CNPJ)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Global Parts Ltd", CNPJ)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldFindSupplierById() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(supplier(id, "Global Parts Ltd")));

        SupplierResponse response = service.findById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("Global Parts Ltd");
    }

    @Test
    void shouldThrowNotFoundWhenSupplierDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldListSuppliersWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Supplier> page = new PageImpl<>(Arrays.asList(
                supplier(UUID.randomUUID(), "Alpha"),
                supplier(UUID.randomUUID(), "Beta")), pageable, 2);
        when(repository.findAll(pageable)).thenReturn(page);

        Page<SupplierResponse> result = service.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(SupplierResponse::getName)
                .containsExactly("Alpha", "Beta");
    }

    @Test
    void shouldUpdateSupplierAndCheckNewDocument() {
        UUID id = UUID.randomUUID();
        Supplier existing = supplier(id, "Old Name");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsByDocument(CPF)).thenReturn(false);
        when(repository.saveAndFlush(any(Supplier.class))).then(returnsFirstArg());

        SupplierResponse response = service.update(id, request("New Name", "529.982.247-25"));

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getDocument()).isEqualTo(CPF);
    }

    @Test
    void shouldNotCheckDuplicateWhenDocumentDidNotChange() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(supplier(id, "Global Parts Ltd")));
        when(repository.saveAndFlush(any(Supplier.class))).then(returnsFirstArg());

        service.update(id, request("Global Parts Updated", "11.222.333/0001-81"));

        verify(repository, never()).existsByDocument(anyString());
    }

    @Test
    void shouldNotUpdateWhenNewDocumentBelongsToAnotherSupplier() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(supplier(id, "Global Parts Ltd")));
        when(repository.existsByDocument(CPF)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, request("Global Parts Ltd", CPF)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).saveAndFlush(any(Supplier.class));
    }

    @Test
    void shouldKeepActiveFlagWhenNotInformatedOnUpdate() {
        UUID id = UUID.randomUUID();
        Supplier existing = supplier(id, "Global Parts Ltd");
        existing.setActive(false);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(Supplier.class))).then(returnsFirstArg());

        SupplierResponse response = service.update(id, request("Global Parts Ltd", CNPJ));

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingSupplier() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request("Global Parts Ltd", CNPJ)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteExistingSupplier() {
        UUID id = UUID.randomUUID();
        Supplier existing = supplier(id, "Global Parts Ltd");
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(repository).delete(existing);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingMissingSupplier() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any(Supplier.class));
    }

    private SupplierRequest request(String name, String document) {
        SupplierRequest request = new SupplierRequest();
        request.setName(name);
        request.setDocument(document);
        return request;
    }

    private Supplier supplier(UUID id, String name) {
        Supplier supplier = new Supplier();
        ReflectionTestUtils.setField(supplier, "id", id);
        supplier.setName(name);
        supplier.setDocument(SupplierServiceTest.CNPJ);
        supplier.setActive(true);
        return supplier;
    }
}
