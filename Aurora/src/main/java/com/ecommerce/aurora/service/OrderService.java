package com.ecommerce.aurora.service;

import com.ecommerce.aurora.payload.OrderDTO;
import com.ecommerce.aurora.payload.OrderRequestDTO;

public interface OrderService {
    OrderDTO placeOrder(String paymentMethod, OrderRequestDTO orderRequestDTO);
}
