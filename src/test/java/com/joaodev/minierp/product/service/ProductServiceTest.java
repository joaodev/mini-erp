package com.joaodev.minierp.product.service;

import com.joaodev.minierp.common.exception.DuplicateResourceException;
import com.joaodev.minierp.common.exception.ResourceNotFoundException;
import com.joaodev.minierp.product.dto.ProductRequest;
import com.joaodev.minierp.product.dto.ProductResponse;
import com.joaodev.minierp.product.entity.Product;
import com.joaodev.minierp.product.mapper.ProductMapper;
import com.joaodev.minierp.product.repository.ProductRepository;
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

import java.math.BigDecimal;
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
public class ProductServiceTest {
    private static final String SKU = "WIDGET-A";
    private static final String OTHER_SKU = "WIDGET-B";

    @Mock
    private ProductRepository repository;

    private ProductService service;

    @Test
    void shouldCreateProductWithNormalizedSkuAndActiveByDefault() {
        when(repository.existsBySku(SKU)).thenReturn(false);
        when(repository.save(any(Product.class))).then(returnsFirstArg());

        ProductResponse response = service.create(request("Widget A", "widget-a", "19.90"));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSku()).isEqualTo(SKU);
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(response.getName()).isEqualTo("Widget A");
        assertThat(response.getSku()).isEqualTo(SKU);
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("19.90"));
    }

    @Test
    void shouldNotCreateProductWhenSkuAlreadyExists() {
        when(repository.existsBySku(SKU)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Widget A", SKU, "19.90")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void shouldFindProductById() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(product(id, "Widget A", SKU, "19.90")));

        ProductResponse response = service.findById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("Widget A");
    }

    @Test
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldListProductsWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(Arrays.asList(
                product(UUID.randomUUID(), "Alpha", SKU, "10.00"),
                product(UUID.randomUUID(), "Beta", OTHER_SKU, "20.00")), pageable, 2);
        when(repository.findAll(pageable)).thenReturn(page);

        Page<ProductResponse> result = service.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(ProductResponse::getName)
                .containsExactly("Alpha", "Beta");
    }

    @Test
    void shouldUpdateProductAndCheckNewSku() {
        UUID id = UUID.randomUUID();
        Product existing = product(id, "Old Name", SKU, "19.90");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsBySku(OTHER_SKU)).thenReturn(false);
        when(repository.saveAndFlush(any(Product.class))).then(returnsFirstArg());

        ProductResponse response = service.update(id, request("New Name", OTHER_SKU, "29.90"));

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getSku()).isEqualTo(OTHER_SKU);
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("29.90"));
    }

    @Test
    void shouldNotCheckDuplicateWhenSkuDidNotChange() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(product(id, "Widget A", SKU, "19.90")));
        when(repository.saveAndFlush(any(Product.class))).then(returnsFirstArg());

        service.update(id, request("Widget A Updated", "widget-a", "19.90"));

        verify(repository, never()).existsBySku(anyString());
    }

    @Test
    void shouldNotUpdateWhenNewSkuBelongsToAnotherProduct() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(product(id, "Widget A", SKU, "19.90")));
        when(repository.existsBySku(OTHER_SKU)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, request("Widget A", OTHER_SKU, "19.90")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void shouldKeepActiveFlagWhenNotInformedOnUpdate() {
        UUID id = UUID.randomUUID();
        Product existing = product(id, "Widget A", SKU, "19.90");
        existing.setActive(false);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(Product.class))).then(returnsFirstArg());

        ProductResponse response = service.update(id, request("Widget A", SKU, "19.90"));

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingProduct() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request("Widget A", SKU, "19.90")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteExistingProduct() {
        UUID id = UUID.randomUUID();
        Product existing = product(id, "Widget A", SKU, "19.90");
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(repository).delete(existing);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingMissingProduct() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any(Product.class));
    }

    @BeforeEach
    void setUp() {
        service = new ProductService(repository, new ProductMapper());
    }

    private ProductRequest request(String name, String sku, String price) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setSku(sku);
        request.setPrice(new BigDecimal(price));
        return request;
    }

    private Product product(UUID id, String name, String sku, String price) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName(name);
        product.setSku(sku);
        product.setPrice(new BigDecimal(price));
        product.setActive(true);
        return product;
    }
}
