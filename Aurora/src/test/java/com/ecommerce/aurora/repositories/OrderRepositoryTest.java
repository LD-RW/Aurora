package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Order;
import com.ecommerce.aurora.model.OrderItem;
import com.ecommerce.aurora.model.Payment;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void savesAndReloadsAnOrderWithItsItemAndPaymentThroughTheirOwnRepositories() {
        User user = userRepository.save(new User("orderRepoUser", "password12345", "orderrepo@example.com"));

        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(user);
        Address savedAddress = addressRepository.save(address);

        Category category = categoryRepository.save(new Category(null, "Electronics"));
        Product product = new Product();
        product.setProductName("Gaming Laptop");
        product.setDescription("A powerful gaming laptop");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(1500));
        product.setDiscount(BigDecimal.ZERO);
        product.setSpecialPrice(BigDecimal.valueOf(1500));
        product.setCategory(category);
        Product savedProduct = productRepository.save(product);

        Payment savedPayment = paymentRepository.save(new Payment("card", "pg-123", "success", "ok", "stripe"));

        Order order = new Order();
        order.setUser(user);
        order.setAddress(savedAddress);
        order.setPayment(savedPayment);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(BigDecimal.valueOf(1500));
        order.setOrderStatus("Order Accepted");
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(savedOrder);
        orderItem.setProduct(savedProduct);
        orderItem.setQuantity(1);
        orderItem.setDiscount(BigDecimal.ZERO);
        orderItem.setOrderedProductPrice(BigDecimal.valueOf(1500));
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);

        assertThat(orderRepository.findById(savedOrder.getOrderId())).isPresent();
        assertThat(orderItemRepository.findById(savedOrderItem.getOrderItemId())).isPresent();
        assertThat(paymentRepository.findById(savedPayment.getPaymentId())).isPresent();
    }
}
