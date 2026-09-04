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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @Value("${project.image}")
    private String imagePath;

    private final List<String> uploadedTestImageFileNames = new ArrayList<>();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // Uploaded files land on the real filesystem, not the transactional H2 database,
    // so @Transactional's rollback never cleans them up -- without this, every test
    // run would leave another stray file behind in the images directory.
    @AfterEach
    void deleteUploadedTestImages() throws IOException {
        for (String fileName : uploadedTestImageFileNames) {
            Files.deleteIfExists(Paths.get(imagePath, fileName));
        }
        uploadedTestImageFileNames.clear();
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

    @Test
    void searchFindsProductsByCaseInsensitiveSubstring() throws Exception {
        Category category = categoryRepository.saveAndFlush(new Category(null, "Search Electronics"));

        Product laptop = new Product();
        laptop.setProductName("Gaming Laptop");
        laptop.setDescription("A powerful laptop");
        laptop.setQuantity(10);
        laptop.setPrice(BigDecimal.valueOf(1500));
        laptop.setDiscount(BigDecimal.ZERO);
        laptop.setSpecialPrice(BigDecimal.valueOf(1500));
        laptop.setCategory(category);
        productRepository.saveAndFlush(laptop);

        Product mouse = new Product();
        mouse.setProductName("Wireless Mouse");
        mouse.setDescription("An ergonomic mouse");
        mouse.setQuantity(20);
        mouse.setPrice(BigDecimal.valueOf(50));
        mouse.setDiscount(BigDecimal.ZERO);
        mouse.setSpecialPrice(BigDecimal.valueOf(50));
        mouse.setCategory(category);
        productRepository.saveAndFlush(mouse);

        mockMvc.perform(get("/api/public/products/search?keyword=laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Gaming Laptop"));
    }

    @Test
    void retrievesAnUploadedProductImageByteForByte() throws Exception {
        Category category = categoryRepository.saveAndFlush(new Category(null, "Image Electronics"));

        Product product = new Product();
        product.setProductName("Image Test Laptop");
        product.setDescription("A laptop for testing image retrieval");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(1000));
        product.setDiscount(BigDecimal.ZERO);
        product.setSpecialPrice(BigDecimal.valueOf(1000));
        product.setImage("default.png");
        product.setCategory(category);
        Product savedProduct = productRepository.saveAndFlush(product);

        authenticateAsAdmin();

        byte[] fakeImageBytes = "fake-png-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile uploadedFile = new MockMultipartFile(
                "Image", "laptop.png", MediaType.IMAGE_PNG_VALUE, fakeImageBytes);

        String responseBody = mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/products/" + savedProduct.getProductId() + "/image")
                        .file(uploadedFile))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String uploadedFileName = objectMapper.readTree(responseBody).get("image").asText();
        uploadedTestImageFileNames.add(uploadedFileName);

        // Content-type detection (Files.probeContentType) depends on the OS's mime
        // database, which isn't guaranteed identical across CI's ubuntu/windows/macos
        // runners -- so this only pins down what's actually guaranteed: the exact
        // bytes that were uploaded come back unchanged.
        mockMvc.perform(get("/api/public/products/image/" + uploadedFileName))
                .andExpect(status().isOk())
                .andExpect(content().bytes(fakeImageBytes));
    }

    @Test
    void rejectsARequestForANonexistentImageAsNotFound() throws Exception {
        mockMvc.perform(get("/api/public/products/image/does-not-exist.png"))
                .andExpect(status().isNotFound());
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
