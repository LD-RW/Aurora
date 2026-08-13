package com.ecommerce.aurora.service;

import com.ecommerce.aurora.payload.CartDTO;

import java.util.List;

public interface CartService {
    CartDTO addProductToCart(Long productId, Integer quantity);

    List<CartDTO> getAllCarts();

    CartDTO getCart(Long cartId);
}
