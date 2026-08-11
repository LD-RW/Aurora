package com.ecommerce.aurora.service;

import com.ecommerce.aurora.payload.CartDTO;

public interface CartService {
    CartDTO addProductToCart(Long productId, Integer quantity);
}
