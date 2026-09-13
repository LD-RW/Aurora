package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Category;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * price, discount, and quantity carried no constraints, so a product could be created with a
 * negative price, negative stock, or a discount above 100 (which made specialPrice negative) or
 * below 0 (which made specialPrice exceed the list price). Omitting price entirely was worse
 * still: it stored null, and adding that product to a cart then failed with a 500 when the null
 * reached BigDecimal.multiply.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductValidationTest {

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
    void rejectsANegativePrice() throws Exception {
        postProduct(validProduct(dto -> dto.setPrice(BigDecimal.valueOf(-100))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").value("Price cannot be negative"));

        assertNothingWasStored();
    }

    @Test
    void rejectsANegativeQuantity() throws Exception {
        postProduct(validProduct(dto -> dto.setQuantity(-50)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").value("Quantity cannot be negative"));

        assertNothingWasStored();
    }

    @Test
    void rejectsADiscountAboveOneHundredWhichWouldMakeSpecialPriceNegative() throws Exception {
        postProduct(validProduct(dto -> dto.setDiscount(BigDecimal.valueOf(150))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.discount").value("Discount cannot exceed 100"));

        assertNothingWasStored();
    }

    @Test
    void rejectsANegativeDiscountWhichWouldChargeAboveTheListPrice() throws Exception {
        postProduct(validProduct(dto -> dto.setDiscount(BigDecimal.valueOf(-50))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.discount").value("Discount cannot be negative"));

        assertNothingWasStored();
    }

    @Test
    void rejectsAMissingPriceRatherThanStoringNull() throws Exception {
        postProduct(validProduct(dto -> dto.setPrice(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").value("Price is required"));

        assertNothingWasStored();
    }

    @Test
    void stillAcceptsAValidProductAndComputesSpecialPrice() throws Exception {
        postProduct(validProduct(dto -> { }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialPrice").value(90.0));
    }

    private org.springframework.test.web.servlet.ResultActions postProduct(ProductDTO productDTO) throws Exception {
        Category category = categoryRepository.saveAndFlush(new Category(null, "Validation Category"));
        authenticateAsAdmin();

        return mockMvc.perform(post("/api/admin/categories/" + category.getCategoryId() + "/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)));
    }

    private ProductDTO validProduct(java.util.function.Consumer<ProductDTO> customizer) {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setProductName("Validation Product");
        productDTO.setDescription("A product used to exercise numeric validation");
        productDTO.setQuantity(10);
        productDTO.setPrice(BigDecimal.valueOf(100));
        productDTO.setDiscount(BigDecimal.TEN);
        customizer.accept(productDTO);
        return productDTO;
    }

    private void assertNothingWasStored() {
        assertThat(productRepository.findByProductName("Validation Product")).isNull();
    }

    private void authenticateAsAdmin() {
        Role adminRole = roleRepository.save(new Role(AppRole.ROLE_ADMIN));
        User admin = new User("validationAdmin", "password12345", "validationadmin@example.com");
        admin.setRoles(Set.of(adminRole));
        userRepository.saveAndFlush(admin);

        UserDetailsImpl principal = UserDetailsImpl.build(admin);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
