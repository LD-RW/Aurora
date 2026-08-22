package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.CategoryDTO;
import com.ecommerce.aurora.repositories.CategoryRepository;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerTest {

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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ignoresAClientSuppliedCategoryIdAndNeverOverwritesAnExistingCategory() throws Exception {
        Category existingCategory = categoryRepository.saveAndFlush(new Category(null, "Electronics"));
        authenticateAsAdmin();

        CategoryDTO forgedRequestBody = new CategoryDTO(existingCategory.getCategoryId(), "Forged Category Name");

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgedRequestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(not(existingCategory.getCategoryId().intValue())));

        Category untouchedExistingCategory = categoryRepository.findById(existingCategory.getCategoryId()).orElseThrow();
        assertThat(untouchedExistingCategory.getCategoryName()).isEqualTo("Electronics");
        assertThat(categoryRepository.count()).isEqualTo(2);
    }

    private void authenticateAsAdmin() {
        Role adminRole = roleRepository.save(new Role(AppRole.ROLE_ADMIN));
        User admin = new User("categoryTestAdmin", "password12345", "categoryadmin@example.com");
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        UserDetailsImpl principal = UserDetailsImpl.build(admin);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
