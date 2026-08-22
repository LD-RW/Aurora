package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.ProductDTO;
import com.ecommerce.aurora.repositories.CategoryRepository;
import com.ecommerce.aurora.repositories.ProductRepository;
import com.ecommerce.aurora.repositories.RoleRepository;
import com.ecommerce.aurora.repositories.UserRepository;
import com.ecommerce.aurora.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ignoresAClientSuppliedProductIdAndNeverOverwritesAnExistingProduct() throws Exception {
        Category category = categoryRepository.saveAndFlush(new Category(null, "Electronics"));

        Product existingProduct = new Product();
        existingProduct.setProductName("Existing Laptop");
        existingProduct.setDescription("A perfectly good laptop");
        existingProduct.setQuantity(10);
        existingProduct.setPrice(BigDecimal.valueOf(1000));
        existingProduct.setDiscount(BigDecimal.ZERO);
        existingProduct.setSpecialPrice(BigDecimal.valueOf(1000));
        existingProduct.setCategory(category);
        Product savedExistingProduct = productRepository.saveAndFlush(existingProduct);

        authenticateAsAdmin();

        ProductDTO forgedRequestBody = new ProductDTO(
                "Forged Product", savedExistingProduct.getProductId(), "A forged product description",
                null, null, 5, null, BigDecimal.valueOf(50), BigDecimal.ZERO, null);

        mockMvc.perform(post("/api/admin/categories/" + category.getCategoryId() + "/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgedRequestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(not(savedExistingProduct.getProductId().intValue())));

        Product untouchedExistingProduct = productRepository.findById(savedExistingProduct.getProductId()).orElseThrow();
        assertThat(untouchedExistingProduct.getProductName()).isEqualTo("Existing Laptop");
        assertThat(productRepository.count()).isEqualTo(2);
    }

    private void authenticateAsAdmin() {
        Role adminRole = roleRepository.save(new Role(AppRole.ROLE_ADMIN));
        User admin = new User("productTestAdmin", "password12345", "productadmin@example.com");
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        UserDetailsImpl principal = UserDetailsImpl.build(admin);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
