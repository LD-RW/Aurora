package com.ecommerce.aurora.mapper;

import com.ecommerce.aurora.model.Address;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Order;
import com.ecommerce.aurora.model.OrderItem;
import com.ecommerce.aurora.model.Payment;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.payload.OrderDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void mapsAnOrderWithItsItemAddressAndPaymentToADto() {
        User user = new User("someone", "password12345", "someone@example.com");

        Address address = new Address("Main Street", "Building A", "Amman", "Amman Governorate", "Jordan", "11183");
        address.setAddressId(5L);

        Category category = new Category(1L, "Electronics");
        Product product = new Product();
        product.setProductId(9L);
        product.setProductName("Gaming Laptop");
        product.setDescription("A powerful gaming laptop");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(1500));
        product.setDiscount(BigDecimal.ZERO);
        product.setSpecialPrice(BigDecimal.valueOf(1500));
        product.setCategory(category);

        Payment payment = new Payment("card", "pg-123", "success", "ok", "stripe");
        payment.setPaymentId(3L);

        Order order = new Order();
        order.setOrderId(1L);
        order.setUser(user);
        order.setAddress(address);
        order.setPayment(payment);
        order.setOrderDate(LocalDate.of(2026, 1, 1));
        order.setTotalAmount(BigDecimal.valueOf(1500));
        order.setOrderStatus("Order Accepted");

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(7L);
        orderItem.setProduct(product);
        orderItem.setOrder(order);
        orderItem.setQuantity(1);
        orderItem.setDiscount(BigDecimal.ZERO);
        orderItem.setOrderedProductPrice(BigDecimal.valueOf(1500));
        order.setOrderItems(List.of(orderItem));

        OrderDTO result = orderMapper.orderToOrderDTO(order);

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("someone@example.com");
        assertThat(result.getAddressId()).isEqualTo(5L);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(result.getOrderStatus()).isEqualTo("Order Accepted");

        assertThat(result.getPayment().getPaymentId()).isEqualTo(3L);
        assertThat(result.getPayment().getPaymentMethod()).isEqualTo("card");
        assertThat(result.getPayment().getPgName()).isEqualTo("stripe");

        assertThat(result.getOrderItems()).hasSize(1);
        assertThat(result.getOrderItems().getFirst().getOrderedProductPrice()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(result.getOrderItems().getFirst().getProduct().getProductId()).isEqualTo(9L);
        assertThat(result.getOrderItems().getFirst().getProduct().getProductName()).isEqualTo("Gaming Laptop");
    }
}
