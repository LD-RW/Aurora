package com.ecommerce.aurora.service;

import com.ecommerce.aurora.payload.OrderDTO;
import com.ecommerce.aurora.payload.OrderRequestDTO;
import com.ecommerce.aurora.payload.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderDTO placeOrder(String paymentMethod, OrderRequestDTO orderRequestDTO);

    OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderDTO getOrderById(Long orderId);

    List<OrderDTO> getCurrentUserOrders();
}
