package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Cart;
import com.ecommerce.aurora.model.CartItem;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.OrderRequestDTO;
import com.ecommerce.aurora.repositories.AddressRepository;
import com.ecommerce.aurora.repositories.CartItemRepository;
import com.ecommerce.aurora.repositories.CartRepository;
import com.ecommerce.aurora.repositories.CategoryRepository;
import com.ecommerce.aurora.repositories.OrderRepository;
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
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stock used to be validated only when an item was added to the cart; checkout subtracted
 * unconditionally. Because a cart can sit for as long as the user likes, anything that sold out
 * in between still checked out and drove products.quantity negative.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderStockValidationTest {

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

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsCheckoutWhenStockDroppedBelowTheCartQuantityAfterTheItemWasAdded() throws Exception {
        User buyer = persistUser("stockBuyer", "stockbuyer@example.com");
        Product product = persistProductWithStock(5);
        Address address = persistAddressFor(buyer);
        persistCartFor(buyer, product, 5);

        // Everything else sells out before this buyer reaches checkout.
        product.setQuantity(0);
        productRepository.saveAndFlush(product);

        authenticateAs(buyer);

        mockMvc.perform(post("/api/order/users/payments/CashOnDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestFor(address))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("left in stock")));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void neverLetsStockGoNegative() throws Exception {
        User buyer = persistUser("negativeStockBuyer", "negativestock@example.com");
        Product product = persistProductWithStock(5);
        Address address = persistAddressFor(buyer);
        persistCartFor(buyer, product, 5);

        product.setQuantity(2);
        productRepository.saveAndFlush(product);

        authenticateAs(buyer);

        mockMvc.perform(post("/api/order/users/payments/CashOnDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestFor(address))))
                .andExpect(status().isBadRequest());

        assertThat(productRepository.findById(product.getProductId()).orElseThrow().getQuantity())
                .isEqualTo(2);
    }

    @Test
    void stillPlacesTheOrderAndDecrementsStockWhenAvailabilityHoldsUp() throws Exception {
        User buyer = persistUser("happyStockBuyer", "happystock@example.com");
        Product product = persistProductWithStock(5);
        Address address = persistAddressFor(buyer);
        persistCartFor(buyer, product, 3);

        authenticateAs(buyer);

        mockMvc.perform(post("/api/order/users/payments/CashOnDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestFor(address))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(120.0));

        assertThat(productRepository.findById(product.getProductId()).orElseThrow().getQuantity())
                .isEqualTo(2);
    }

    private OrderRequestDTO orderRequestFor(Address address) {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setAddressId(address.getAddressId());
        return request;
    }

    private User persistUser(String username, String email) {
        Role userRole = roleRepository.save(new Role(AppRole.ROLE_USER));
        User user = new User(username, "password12345", email);
        user.setRoles(Set.of(userRole));
        return userRepository.saveAndFlush(user);
    }

    private Product persistProductWithStock(int stock) {
        Category category = categoryRepository.saveAndFlush(new Category(null, "Stock Test Category " + stock));

        Product product = new Product();
        product.setProductName("Stock Test Product " + stock);
        product.setDescription("Product used for checkout stock validation");
        product.setQuantity(stock);
        product.setPrice(BigDecimal.valueOf(40));
        product.setDiscount(BigDecimal.ZERO);
        product.setSpecialPrice(BigDecimal.valueOf(40));
        product.setImage("default.png");
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private Address persistAddressFor(User user) {
        Address address = new Address();
        address.setStreet("123 Stock Street");
        address.setBuildingName("Stock Tower");
        address.setCity("Stockholm");
        address.setState("ST");
        address.setCountry("Testland");
        address.setPinCode("12345");
        address.setUser(user);
        return addressRepository.saveAndFlush(address);
    }

    private void persistCartFor(User user, Product product, int quantity) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalPrice(product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)));
        cart.setItems(new ArrayList<>());
        Cart savedCart = cartRepository.saveAndFlush(cart);

        CartItem cartItem = new CartItem();
        cartItem.setCart(savedCart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setProductPrice(product.getSpecialPrice());
        cartItem.setDiscount(product.getDiscount());
        cartItemRepository.saveAndFlush(cartItem);

        savedCart.getItems().add(cartItem);
        cartRepository.saveAndFlush(savedCart);
    }

    private void authenticateAs(User user) {
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
