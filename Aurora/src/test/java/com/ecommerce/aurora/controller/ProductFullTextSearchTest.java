package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.repositories.CategoryRepository;
import com.ecommerce.aurora.repositories.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// No @Transactional here: InnoDB's FULLTEXT index can't see a row inserted earlier
// in the same, uncommitted transaction (verified against a real MySQL instance), so
// each setup call below must actually commit -- meaning manual cleanup instead of
// this project's usual rollback-per-test pattern.
@Tag("mysql")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql")
class ProductFullTextSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    void cleanUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void matchesMultipleTermsAcrossNameAndDescriptionByPrefix() throws Exception {
        Category category = categoryRepository.saveAndFlush(new Category(null, "Full-Text Electronics"));

        Product mouse = new Product();
        mouse.setProductName("Wireless Mouse");
        mouse.setDescription("An ergonomic mouse for everyday office and gaming use");
        mouse.setQuantity(20);
        mouse.setPrice(BigDecimal.valueOf(50));
        mouse.setDiscount(BigDecimal.ZERO);
        mouse.setSpecialPrice(BigDecimal.valueOf(50));
        mouse.setCategory(category);
        productRepository.saveAndFlush(mouse);

        Product chair = new Product();
        chair.setProductName("Office Chair");
        chair.setDescription("A comfortable chair with no relation to computing at all");
        chair.setQuantity(5);
        chair.setPrice(BigDecimal.valueOf(200));
        chair.setDiscount(BigDecimal.ZERO);
        chair.setSpecialPrice(BigDecimal.valueOf(200));
        chair.setCategory(category);
        productRepository.saveAndFlush(chair);

        mockMvc.perform(get("/api/public/products/search?keyword=mouse office"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Wireless Mouse"));
    }

    @Test
    void returnsAnEmptyPageForANoMatchSearch() throws Exception {
        mockMvc.perform(get("/api/public/products/search?keyword=doesnotexistanywhere"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
