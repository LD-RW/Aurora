package com.ecommerce.aurora.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Out-of-range pagination values reached PageRequest.of, which throws IllegalArgumentException
 * for a negative page number or a size below 1. With no handler for it, that surfaced as a 500
 * and was logged as an unhandled server error, even though the cause was entirely client input.
 * An unbounded pageSize also let one request ask for a whole table at once.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaginationBoundsTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/public/categories",
            "/api/public/products"
    })
    void rejectsANegativePageNumberAsABadRequest(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint + "?pageNumber=-1"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/public/categories",
            "/api/public/products"
    })
    void rejectsAZeroPageSizeAsABadRequest(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint + "?pageSize=0"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/public/categories",
            "/api/public/products"
    })
    void rejectsANegativePageSizeAsABadRequest(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint + "?pageSize=-5"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/public/categories",
            "/api/public/products"
    })
    void rejectsAPageSizeAboveTheMaximum(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint + "?pageSize=999999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOutOfRangePaginationOnTheSearchEndpointToo() throws Exception {
        mockMvc.perform(get("/api/public/products/search?keyword=anything&pageNumber=-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stillAcceptsValidPaginationValues() throws Exception {
        mockMvc.perform(get("/api/public/categories?pageNumber=0&pageSize=10"))
                .andExpect(status().isOk());
    }

    @Test
    void stillAcceptsAPageBeyondTheLastOneAsAnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/public/categories?pageNumber=99"))
                .andExpect(status().isOk());
    }
}
