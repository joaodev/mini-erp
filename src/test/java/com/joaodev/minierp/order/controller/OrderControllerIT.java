package com.joaodev.minierp.order.controller;

import com.joaodev.minierp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/orders";

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE order_item, orders, customer, product CASCADE");
    }

    @Test
    void shouldCreateOrderAndComputeTotals() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequestBody(customerId, productId, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(59.70))
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void shouldReturnNotFoundWhenCustomerDoesNotExist() throws Exception {
        String productId = createProduct();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequestBody(UUID.randomUUID().toString(), productId, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        String customerId = createCustomer();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequestBody(customerId, UUID.randomUUID().toString(), 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenItemsAreEmpty() throws Exception {
        String customerId = createCustomer();

        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);
        body.put("items", Collections.emptyList());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").value("Order must have at least one item"));
    }

    @Test
    void shouldFindOrderById() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String orderId = createOrder(customerId, productId, 2);

        mockMvc.perform(get(BASE_URL + "/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.totalAmount").value(39.80));
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldConfirmPendingOrder() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String orderId = createOrder(customerId, productId, 1);

        mockMvc.perform(post(BASE_URL + "/" + orderId + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldReturnConflictWhenConfirmingAlreadyConfirmedOrder() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String orderId = createOrder(customerId, productId, 1);

        mockMvc.perform(post(BASE_URL + "/" + orderId + "/confirm")).andExpect(status().isOk());

        mockMvc.perform(post(BASE_URL + "/" + orderId + "/confirm"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldCancelPendingOrder() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String orderId = createOrder(customerId, productId, 1);

        mockMvc.perform(post(BASE_URL + "/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturnConflictWhenCancellingAlreadyCancelledOrder() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String orderId = createOrder(customerId, productId, 1);

        mockMvc.perform(post(BASE_URL + "/" + orderId + "/cancel")).andExpect(status().isOk());

        mockMvc.perform(post(BASE_URL + "/" + orderId + "/cancel"))
                .andExpect(status().isConflict());
    }

    private String createOrder(String customerId, String productId, int quantity) throws Exception {
        String body = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequestBody(customerId, productId, quantity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String orderRequestBody(String customerId, String productId, int quantity) throws Exception {
        Map<String, Object> item = new HashMap<>();
        item.put("productId", productId);
        item.put("quantity", quantity);

        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customerId);
        body.put("items", Collections.singletonList(item));
        return objectMapper.writeValueAsString(body);
    }

    private String createCustomer() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "John Silva");
        body.put("document", "52998224725");
        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createProduct() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Widget A");
        body.put("sku", "WIDGET-A");
        body.put("price", new BigDecimal("19.90"));
        String response = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}