package com.joaodev.minierp.customer.controller;

import com.joaodev.minierp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class CustomerControllerIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/customers";
    private static final String CPF = "52998224725";
    private static final String CNPJ = "11222333000181";

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE customer");
    }

    @Test
    void shouldCreateCustomerAndNormalizeDocument() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("John Silva", "529.982.247-25")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE_URL + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("John Silva"))
                .andExpect(jsonPath("$.document").value(CPF))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturnConflictWhenDocumentAlreadyExists() throws Exception {
        createCustomer("John Silva", CPF);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Other Person", "529.982.247-25")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnBadRequestWhenDocumentIsInvalid() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("John Silva", "111.111.111-11")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.document").value("Invalid CPF or CNPJ"));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(null, CPF)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"));
    }

    @Test
    void shouldFindCustomerById() throws Exception {
        String id = createCustomer("John Silva", CPF);

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("John Silva"));
    }

    @Test
    void shouldReturnNotFoundWhenCustomerDoesNotExist() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldListCustomerWithPagination() throws Exception {
        createCustomer("Alice", CPF);
        createCustomer("Bob", CNPJ);
        createCustomer("Carol", "12ABC34501DE35");

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldUpdateCustomer() throws Exception {
        String id = createCustomer("John Silva", CPF);

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("John Updated", CPF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"));

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(jsonPath("$.name").value("John Updated"));
    }

    @Test
    void shouldReturnConflictWhenUpdatingToDocumentOfAnotherCustomer() throws Exception {
        createCustomer("Alice", CPF);
        String bobId = createCustomer("Bob", CNPJ);

        mockMvc.perform(put(BASE_URL + "/" + bobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Bob", CPF)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldSoftDeleteCustomer() throws Exception {
        String id = createCustomer("John Silva", CPF);

        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());

        Integer softDeletedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE id = ? AND deleted_at IS NOT NULL",
                Integer.class, UUID.fromString(id));
        assertThat(softDeletedRows).isEqualTo(1);
    }

    @Test
    void shouldAllowCreatingSameDocumentAfterSoftDelete() throws Exception {
        String id = createCustomer("John Silva", CPF);
        mockMvc.perform(delete(BASE_URL + "/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("John Again", CPF)))
                .andExpect(status().isCreated());
    }

    private String createCustomer(String name, String document) throws Exception {
        String body = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(name, document)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String requestBody(String name, String document) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("document", document);
        return objectMapper.writeValueAsString(body);
    }
}
