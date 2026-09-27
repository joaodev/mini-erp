package com.joaodev.minierp.product.controller;

import com.joaodev.minierp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/products";
    private static final String SKU = "WIDGET-A";
    private static final String OTHER_SKU = "WIDGET-B";

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE product");
    }

    @Test
    void shouldCreateProductAndNormalizeSku() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Widget A", "widget-a", "19.90")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE_URL + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Widget A"))
                .andExpect(jsonPath("$.sku").value(SKU))
                .andExpect(jsonPath("$.price").value(19.90))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturnConflictWhenSkuAlreadyExists() throws Exception {
        createProduct("Widget A", SKU, "19.90");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Other", "widget-a", "9.90")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnBadRequestWhenPriceIsZeroOrNegative() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Bad", "BAD-1", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").value("Price must be greater than zero"));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(null, SKU, "19.90")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"));
    }

    @Test
    void shouldFindProductById() throws Exception {
        String id = createProduct("Widget A", SKU, "19.90");

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Widget A"));
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldListProductsWithPagination() throws Exception {
        createProduct("Alpha", SKU, "10.00");
        createProduct("Beta", OTHER_SKU, "20.00");
        createProduct("Gamma", "WIDGET-C", "30.00");

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        String id = createProduct("Widget A", SKU, "19.90");

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Widget A Updated", SKU, "24.90")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Widget A Updated"))
                .andExpect(jsonPath("$.price").value(24.90));

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(jsonPath("$.name").value("Widget A Updated"));
    }

    @Test
    void shouldReturnConflictWhenUpdatingToSkuOfAnotherProduct() throws Exception {
        createProduct("Alpha", SKU, "10.00");
        String betaId = createProduct("Beta", OTHER_SKU, "20.00");

        mockMvc.perform(put(BASE_URL + "/" + betaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Beta", SKU, "20.00")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldSoftDeleteProduct() throws Exception {
        String id = createProduct("Widget A", SKU, "19.90");

        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());

        Integer softDeletedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE id = ? AND deleted_at IS NOT NULL",
                Integer.class, UUID.fromString(id));
        assertThat(softDeletedRows).isEqualTo(1);
    }

    @Test
    void shouldAllowCreatingSameSkuAfterSoftDelete() throws Exception {
        String id = createProduct("Widget A", SKU, "19.90");
        mockMvc.perform(delete(BASE_URL + "/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Widget A Again", SKU, "19.90")))
                .andExpect(status().isCreated());
    }

    private String createProduct(String name, String sku, String price) throws Exception {
        String body = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(name, sku, price)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String requestBody(String name, String sku, String price) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("sku", sku);
        body.put("price", price == null ? null : new BigDecimal(price));
        return objectMapper.writeValueAsString(body);
    }
}