package com.ecommerce.aurora.model;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class OrderEntityRelationshipTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsAndReloadsAnOrderWithItsItemAddressPaymentAndUser() {
        User user = new User("orderTestUser", "password12345", "ordertest@example.com");
        entityManager.persist(user);

        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setUser(user);
        entityManager.persist(address);

        Category category = new Category(null, "Electronics");
        entityManager.persist(category);

        Product product = new Product();
        product.setProductName("Gaming Laptop");
        product.setDescription("A powerful gaming laptop");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(1500));
        product.setDiscount(BigDecimal.ZERO);
        product.setSpecialPrice(BigDecimal.valueOf(1500));
        product.setCategory(category);
        entityManager.persist(product);

        Payment payment = new Payment("card", "pg-123", "success", "ok", "stripe");
        entityManager.persist(payment);

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(BigDecimal.valueOf(1500));
        order.setOrderStatus("Order Accepted");
        order.setAddress(address);
        order.setPayment(payment);
        entityManager.persist(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setOrder(order);
        orderItem.setQuantity(1);
        orderItem.setDiscount(BigDecimal.ZERO);
        orderItem.setOrderedProductPrice(BigDecimal.valueOf(1500));
        entityManager.persist(orderItem);

        entityManager.flush();
        entityManager.clear();

        Order reloadedOrder = entityManager.find(Order.class, order.getOrderId());
        assertThat(reloadedOrder.getUser().getUserId()).isEqualTo(user.getUserId());
        assertThat(reloadedOrder.getAddress().getAddressId()).isEqualTo(address.getAddressId());
        assertThat(reloadedOrder.getPayment().getPaymentId()).isEqualTo(payment.getPaymentId());
        assertThat(reloadedOrder.getOrderItems()).hasSize(1);
        assertThat(reloadedOrder.getOrderItems().getFirst().getProduct().getProductId()).isEqualTo(product.getProductId());
        assertThat(reloadedOrder.getOrderItems().getFirst().getOrderedProductPrice()).isEqualByComparingTo(BigDecimal.valueOf(1500));

        User reloadedUser = entityManager.find(User.class, user.getUserId());
        assertThat(reloadedUser.getOrders())
                .extracting(Order::getOrderId)
                .containsExactly(order.getOrderId());
    }

    @Test
    void rejectsAPaymentWithATooShortPaymentMethod() {
        Payment payment = new Payment("ab", "pg-1", "success", "ok", "stripe");

        assertThatThrownBy(() -> entityManager.persistAndFlush(payment))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
