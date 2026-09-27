package com.joaodev.minierp.supplier.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SupplierControllerIT extends AbstractIntegrationTest {
    private static final String BASE_URL = "/api/v1/suppliers";
    private static final String CPF = "52998224725";
    private static final String CNPJ = "11222333000181";

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE supplier");
    }

    @Test
    void shouldCreateSupplierAndNormalizeDocument() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Global Parts Ltd", "11.222.333/0001-81")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE_URL + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Global Parts Ltd"))
                .andExpect(jsonPath("$.document").value(CNPJ))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturnConflictWhenDocumentAlreadyExists() throws Exception {
        createSupplier("Global Parts Ltd", CNPJ);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Other Supplier", "11.222.333/0001-81")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnBadRequestWhenDocumentIsInvalid() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Global Parts Ltd", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.document").value("Invalid CPF or CNPJ"));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(null, CNPJ)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"));
    }

    @Test
    void shouldFindSupplierById() throws Exception {
        String id = createSupplier("Global Parts Ltd", CNPJ);

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Global Parts Ltd"));
    }

    @Test
    void shouldReturnNotFoundWhenSupplierDoesNotExist() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldListSuppliersWithPagination() throws Exception {
        createSupplier("Alpha", CNPJ);
        createSupplier("Beta", CPF);
        createSupplier("Gamma", "12ABC34501DE35");

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldUpdateSupplier() throws Exception {
        String id = createSupplier("Global Parts Ltd", CNPJ);

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Global Parts Updated", CNPJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Global Parts Updated"));

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(jsonPath("$.name").value("Global Parts Updated"));
    }

    @Test
    void shouldReturnConflictWhenUpdatingToDocumentOfAnotherSupplier() throws Exception {
        createSupplier("Alpha", CNPJ);
        String betaId = createSupplier("Beta", CPF);

        mockMvc.perform(put(BASE_URL + "/" + betaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Beta", CNPJ)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldSoftDeleteSupplier() throws Exception {
        String id = createSupplier("Global Parts Ltd", CNPJ);

        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());

        Integer softDeletedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM supplier WHERE id = ? AND deleted_at IS NOT NULL",
                Integer.class, UUID.fromString(id));
        assertThat(softDeletedRows).isEqualTo(1);
    }

    @Test
    void shouldAllowCreatingSameDocumentAfterSoftDelete() throws Exception {
        String id = createSupplier("Global Parts Ltd", CNPJ);
        mockMvc.perform(delete(BASE_URL + "/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("Global Parts Again", CNPJ)))
                .andExpect(status().isCreated());
    }

    private String createSupplier(String name, String document) throws Exception {
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
