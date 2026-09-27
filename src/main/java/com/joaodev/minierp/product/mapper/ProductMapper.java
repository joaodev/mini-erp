package com.joaodev.minierp.product.mapper;

import com.joaodev.minierp.product.dto.ProductRequest;
import com.joaodev.minierp.product.dto.ProductResponse;
import com.joaodev.minierp.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public Product toEntity(ProductRequest request, String normalizedSku) {
        Product product = new Product();
        product.setName(request.getName());
        product.setSku(normalizedSku);
        product.setPrice(request.getPrice());
        product.setActive(request.getActive() == null || request.getActive());
        return product;
    }

    public void updateEntity(Product product, ProductRequest request, String normalizedSku) {
        product.setName(request.getName());
        product.setSku(normalizedSku);
        product.setPrice(request.getPrice());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
    }

    public ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setSku(product.getSku());
        response.setPrice(product.getPrice());
        response.setActive(product.isActive());
        response.setVersion(product.getVersion());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }
}
