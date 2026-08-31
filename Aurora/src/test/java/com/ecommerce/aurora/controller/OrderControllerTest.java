package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Cart;
import com.ecommerce.aurora.model.CartItem;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Order;
import com.ecommerce.aurora.model.OrderItem;
import com.ecommerce.aurora.model.Payment;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.OrderRequestDTO;
import com.ecommerce.aurora.repositories.AddressRepository;
import com.ecommerce.aurora.repositories.CartItemRepository;
import com.ecommerce.aurora.repositories.CartRepository;
import com.ecommerce.aurora.repositories.CategoryRepository;
import com.ecommerce.aurora.repositories.OrderItemRepository;
import com.ecommerce.aurora.repositories.OrderRepository;
import com.ecommerce.aurora.repositories.PaymentRepository;
import com.ecommerce.aurora.repositories.ProductRepository;
import com.ecommerce.aurora.repositories.RoleRepository;
import com.ecommerce.aurora.repositories.UserRepository;
import com.ecommerce.aurora.security.services.UserDetailsImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void placesAnOrderFromTheCurrentUsersCartAndClearsIt() throws Exception {
        User user = userRepository.save(new User("orderPlacer", "password12345", "orderplacer@example.com"));
        authenticateAs(user);

        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(user);
        Address savedAddress = addressRepository.saveAndFlush(address);

        Product product = createProduct("Gaming Laptop", 10, BigDecimal.valueOf(1500));
        addToCart(user, product, 2);

        OrderRequestDTO requestBody = new OrderRequestDTO(savedAddress.getAddressId(), "stripe", "pg-123", "success", "ok");

        mockMvc.perform(post("/api/order/users/payments/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("orderplacer@example.com"))
                .andExpect(jsonPath("$.addressId").value(savedAddress.getAddressId()))
                .andExpect(jsonPath("$.orderStatus").value("Order Accepted"))
                .andExpect(jsonPath("$.payment.paymentMethod").value("card"))
                .andExpect(jsonPath("$.payment.pgName").value("stripe"))
                .andExpect(jsonPath("$.orderItems.length()").value(1))
                .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
                .andExpect(jsonPath("$.orderItems[0].product.productId").value(product.getProductId()));

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertThat(updatedProduct.getQuantity()).isEqualTo(8);

        Cart cart = cartRepository.findCartByEmail(user.getEmail());
        assertThat(cart.getItems()).isEmpty();

        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void placesAnOrderWithMultipleCartItemsAndConvertsEveryOne() throws Exception {
        User user = userRepository.save(new User("multiItemOrderer", "password12345", "multiitem@example.com"));
        authenticateAs(user);

        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(user);
        Address savedAddress = addressRepository.saveAndFlush(address);

        Product firstProduct = createProduct("Gaming Laptop", 10, BigDecimal.valueOf(1500));
        Product secondProduct = createProduct("Wireless Mouse", 20, BigDecimal.valueOf(50));
        Product thirdProduct = createProduct("Mechanical Keyboard", 15, BigDecimal.valueOf(100));
        addToCart(user, firstProduct, 1);
        addToCart(user, secondProduct, 2);
        addToCart(user, thirdProduct, 1);

        OrderRequestDTO requestBody = new OrderRequestDTO(savedAddress.getAddressId(), "stripe", "pg-123", "success", "ok");

        mockMvc.perform(post("/api/order/users/payments/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderItems.length()").value(3));

        Cart cart = cartRepository.findCartByEmail(user.getEmail());
        assertThat(cart.getItems()).isEmpty();

        assertThat(productRepository.findById(firstProduct.getProductId()).orElseThrow().getQuantity()).isEqualTo(9);
        assertThat(productRepository.findById(secondProduct.getProductId()).orElseThrow().getQuantity()).isEqualTo(18);
        assertThat(productRepository.findById(thirdProduct.getProductId()).orElseThrow().getQuantity()).isEqualTo(14);
    }

    @Test
    void rejectsPlacingAnOrderWithAnEmptyCart() throws Exception {
        User user = userRepository.save(new User("emptyCartUser", "password12345", "emptycart@example.com"));
        authenticateAs(user);

        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(user);
        Address savedAddress = addressRepository.saveAndFlush(address);

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepository.saveAndFlush(cart);

        OrderRequestDTO requestBody = new OrderRequestDTO(savedAddress.getAddressId(), "stripe", "pg-123", "success", "ok");

        mockMvc.perform(post("/api/order/users/payments/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart is empty"));
    }

    @Test
    void rejectsPlacingAnOrderWhenTheUserHasNoCartAtAll() throws Exception {
        User user = userRepository.save(new User("noCartUser", "password12345", "nocart@example.com"));
        authenticateAs(user);

        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(user);
        Address savedAddress = addressRepository.saveAndFlush(address);

        OrderRequestDTO requestBody = new OrderRequestDTO(savedAddress.getAddressId(), "stripe", "pg-123", "success", "ok");

        mockMvc.perform(post("/api/order/users/payments/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsPlacingAnOrderWithANonexistentAddress() throws Exception {
        User user = userRepository.save(new User("badAddressUser", "password12345", "badaddress@example.com"));
        authenticateAs(user);

        Product product = createProduct("Gaming Laptop", 10, BigDecimal.valueOf(1500));
        addToCart(user, product, 1);

        OrderRequestDTO requestBody = new OrderRequestDTO(999999L, "stripe", "pg-123", "success", "ok");

        mockMvc.perform(post("/api/order/users/payments/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Address not found with addressId: 999999"));
    }

    @Test
    void rejectsPlacingAnOrderWithAnotherUsersAddress() throws Exception {
        User addressOwner = userRepository.save(new User("addressOwnerVictim", "password12345", "addressownervictim@example.com"));
        Address victimsAddress = new Address("Victim Street", "Victim Building", "Amman", "Amman Governorate", "Jordan", "11183");
        victimsAddress.setUser(addressOwner);
        Address savedVictimsAddress = addressRepository.saveAndFlush(victimsAddress);

        User attacker = userRepository.save(new User("addressAttacker", "password12345", "addressattacker@example.com"));
        authenticateAs(attacker);

        Product product = createProduct("Gaming Laptop", 10, BigDecimal.valueOf(1500));
        addToCart(attacker, product, 1);

        OrderRequestDTO requestBody = new OrderRequestDTO(savedVictimsAddress.getAddressId(), "stripe", "pg-123", "success", "ok");

        mockMvc.perform(post("/api/order/users/payments/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + savedVictimsAddress.getAddressId()));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void rejectsAnInvalidPaymentMethodWithAClientErrorNotAServerError() throws Exception {
        User user = userRepository.save(new User("shortPayMethodUser", "password12345", "shortpaymentmethod@example.com"));
        authenticateAs(user);

        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(user);
        Address savedAddress = addressRepository.saveAndFlush(address);

        Product product = createProduct("Gaming Laptop", 10, BigDecimal.valueOf(1500));
        addToCart(user, product, 1);

        OrderRequestDTO requestBody = new OrderRequestDTO(savedAddress.getAddressId(), "stripe", "pg-123", "success", "ok");

        // No orderRepository.count() follow-up here: once the ConstraintViolationException
        // fires mid-flush, this test's own shared transaction is left in a state where any
        // further query on the same session re-triggers the same failed flush. A real
        // request never shares a transaction with anything after it, so this is purely a
        // test-harness artifact -- the point of this test is the status code, not a second
        // query through the same broken session.
        mockMvc.perform(post("/api/order/users/payments/x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnInvalidPayloadMissingAddressId() throws Exception {
        User user = userRepository.save(new User("missingAddressIdUser", "password12345", "missingaddressid@example.com"));
        authenticateAs(user);

        OrderRequestDTO requestBody = new OrderRequestDTO(null, "stripe", "pg-123", "success", "ok");

        mockMvc.perform(post("/api/order/users/payments/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnUnauthenticatedRequest() throws Exception {
        OrderRequestDTO requestBody = new OrderRequestDTO(1L, "stripe", "pg-123", "success", "ok");

        mockMvc.perform(post("/api/order/users/payments/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsAllOrdersAcrossEveryUserForAnAdminWithWorkingPagination() throws Exception {
        User firstUser = userRepository.save(new User("listOrdersFirst", "password12345", "listordersfirst@example.com"));
        User secondUser = userRepository.save(new User("listOrdersSecond", "password12345", "listorderssecond@example.com"));
        User thirdUser = userRepository.save(new User("listOrdersThird", "password12345", "listordersthird@example.com"));

        Product firstProduct = createProduct("Gaming Laptop", 10, BigDecimal.valueOf(1500));
        Product secondProduct = createProduct("Wireless Mouse", 20, BigDecimal.valueOf(50));
        Product thirdProduct = createProduct("Mechanical Keyboard", 15, BigDecimal.valueOf(100));

        createOrder(firstUser, firstProduct, 1, BigDecimal.valueOf(1500));
        createOrder(secondUser, secondProduct, 2, BigDecimal.valueOf(50));
        createOrder(thirdUser, thirdProduct, 3, BigDecimal.valueOf(100));

        authenticateAsAdmin();

        // Page 1 of 2 (pageSize=2): proves this is real database-level pagination, not
        // the whole result set fetched and sliced in memory -- and that each row's
        // nested orderItems/product data still comes back correctly despite that.
        mockMvc.perform(get("/api/admin/orders?pageNumber=0&pageSize=2&sortBy=orderId&sortOrder=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.lastPage").value(false))
                .andExpect(jsonPath("$.content[0].orderItems[0].product.productName").value("Gaming Laptop"))
                .andExpect(jsonPath("$.content[0].orderItems[0].quantity").value(1))
                .andExpect(jsonPath("$.content[1].orderItems[0].product.productName").value("Wireless Mouse"))
                .andExpect(jsonPath("$.content[1].orderItems[0].quantity").value(2));

        // Page 2 of 2: the remaining order, not a repeat of page 1's content.
        mockMvc.perform(get("/api/admin/orders?pageNumber=1&pageSize=2&sortBy=orderId&sortOrder=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.lastPage").value(true))
                .andExpect(jsonPath("$.content[0].orderItems[0].product.productName").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.content[0].orderItems[0].quantity").value(3));
    }

    @Test
    void returnsAnEmptyPageWhenThereAreNoOrders() throws Exception {
        authenticateAsAdmin();

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsAnInvalidSortFieldWhenListingAllOrders() throws Exception {
        authenticateAsAdmin();

        mockMvc.perform(get("/api/admin/orders?sortBy=notARealField"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnAuthenticatedNonAdminUserFromListingAllOrders() throws Exception {
        User user = userRepository.save(new User("listOrdersNonAdmin", "password12345", "listordersnonadmin@example.com"));
        authenticateAs(user);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAnUnauthenticatedRequestToListAllOrders() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    private Order createOrder(User user, Product product, int quantity, BigDecimal pricePerUnit) {
        Address address = new Address("Order Street", "Order Building", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(user);
        Address savedAddress = addressRepository.saveAndFlush(address);

        Payment payment = paymentRepository.saveAndFlush(new Payment("card", "pg-1", "success", "ok", "stripe"));

        Order order = new Order();
        order.setUser(user);
        order.setAddress(savedAddress);
        order.setPayment(payment);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(pricePerUnit.multiply(BigDecimal.valueOf(quantity)));
        order.setOrderStatus("Order Accepted");
        Order savedOrder = orderRepository.saveAndFlush(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(savedOrder);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setDiscount(BigDecimal.ZERO);
        orderItem.setOrderedProductPrice(pricePerUnit);
        orderItemRepository.saveAndFlush(orderItem);

        // savedOrder was built with `new Order()`, so its orderItems field is still the
        // plain, un-Hibernate-managed ArrayList from the field initializer -- it was
        // never replaced with a real PersistentCollection the way a genuinely queried
        // entity's would be. Once an entity's ID is known to the persistence context (as
        // soon as it's saved, not only once it's loaded), Hibernate always trusts
        // whatever is already sitting in that field over a later query's result for the
        // same row -- so without detaching here, the admin list-orders endpoint's own
        // fetch-join query later in this same test transaction would find this exact
        // instance already present and leave its still-plain, still-empty orderItems
        // list untouched, instead of populating it from the query that just fetched it.
        // A real request never has this problem: Order rows only ever get loaded via a
        // query there, never constructed with `new Order()` and then queried again.
        entityManager.detach(savedOrder);

        return savedOrder;
    }

    private void authenticateAsAdmin() {
        Role adminRole = roleRepository.save(new Role(AppRole.ROLE_ADMIN));
        User admin = new User("orderTestAdmin", "password12345", "orderadmin@example.com");
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        authenticateAs(admin);
    }

    private Product createProduct(String name, int stock, BigDecimal price) {
        Category category = categoryRepository.save(new Category(null, "Electronics-" + name));
        Product product = new Product();
        product.setProductName(name);
        product.setDescription("A great product description");
        product.setQuantity(stock);
        product.setPrice(price);
        product.setDiscount(BigDecimal.ZERO);
        product.setSpecialPrice(price);
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private void addToCart(User user, Product product, int quantity) {
        Cart cart = cartRepository.findCartByEmail(user.getEmail());
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cart.setTotalPrice(BigDecimal.ZERO);
            cart = cartRepository.saveAndFlush(cart);
        }

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setProductPrice(product.getSpecialPrice());
        cartItem.setDiscount(product.getDiscount());
        cartItemRepository.saveAndFlush(cartItem);

        cart.setTotalPrice(cart.getTotalPrice().add(product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity))));
        cartRepository.saveAndFlush(cart);

        // findCartByEmail's JOIN FETCH only populates cart.items the *first* time this
        // Cart instance is loaded into the persistence context -- the lookup above (to
        // decide whether to create a new cart) already did that while items was still
        // empty. Without detaching it here, OrderServiceImpl's own findCartByEmail call
        // later in this same test transaction would reuse that stale, empty collection
        // instead of seeing the items just added. A real request never hits this: each
        // one gets its own fresh persistence context, so this is purely a test artifact.
        entityManager.detach(cart);
    }

    private void authenticateAs(User user) {
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
