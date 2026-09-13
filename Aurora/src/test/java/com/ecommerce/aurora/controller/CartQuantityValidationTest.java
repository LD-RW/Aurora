package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.repositories.CartRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A negative quantity used to pass the stock guard in addProductToCart, because that guard
 * reads "product.getQuantity() < quantity" -- for a stock of 5 and a quantity of -5, "5 < -5"
 * is false. The resulting cart line inverted the cart total, and checking out then *raised*
 * stock instead of lowering it, since placeOrder subtracts the line quantity.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartQuantityValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsANegativeQuantityInsteadOfCreatingAnInvertedCartLine() throws Exception {
        Product product = persistProductWithStock(5);
        authenticateAsUser();

        mockMvc.perform(post("/api/carts/products/" + product.getProductId() + "/quantity/-5"))
                .andExpect(status().isBadRequest());

        assertThat(cartRepository.findCartByEmail("cartqty@example.com")).isNull();
    }

    @Test
    void rejectsAZeroQuantityInsteadOfCreatingAnEmptyCartLine() throws Exception {
        Product product = persistProductWithStock(5);
        authenticateAsUser();

        mockMvc.perform(post("/api/carts/products/" + product.getProductId() + "/quantity/0"))
                .andExpect(status().isBadRequest());

        assertThat(cartRepository.findCartByEmail("cartqty@example.com")).isNull();
    }

    @Test
    void stillAcceptsAPositiveQuantityWithinStock() throws Exception {
        Product product = persistProductWithStock(5);
        authenticateAsUser();

        mockMvc.perform(post("/api/carts/products/" + product.getProductId() + "/quantity/2"))
                .andExpect(status().isCreated());

        assertThat(cartRepository.findCartByEmail("cartqty@example.com").getTotalPrice())
                .isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    @Test
    void stillRejectsAQuantityAboveAvailableStock() throws Exception {
        Product product = persistProductWithStock(5);
        authenticateAsUser();

        mockMvc.perform(post("/api/carts/products/" + product.getProductId() + "/quantity/6"))
                .andExpect(status().isBadRequest());
    }

    private Product persistProductWithStock(int stock) {
        Category category = categoryRepository.saveAndFlush(new Category(null, "Cart Quantity Footwear"));

        Product product = new Product();
        product.setProductName("Cart Quantity Boots");
        product.setDescription("Boots used for cart quantity validation");
        product.setQuantity(stock);
        product.setPrice(BigDecimal.valueOf(40));
        product.setDiscount(BigDecimal.ZERO);
        product.setSpecialPrice(BigDecimal.valueOf(40));
        product.setImage("default.png");
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private void authenticateAsUser() {
        Role userRole = roleRepository.save(new Role(AppRole.ROLE_USER));
        User user = new User("cartQtyUser", "password12345", "cartqty@example.com");
        user.setRoles(Set.of(userRole));
        userRepository.saveAndFlush(user);

        UserDetailsImpl principal = UserDetailsImpl.build(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
