package com.ecommerce.aurora.service;

import com.ecommerce.aurora.payload.APIResponse;
import com.ecommerce.aurora.payload.CartDTO;

import java.util.List;

public interface CartService {
    CartDTO addProductToCart(Long productId, Integer quantity);

    List<CartDTO> getAllCarts();

    CartDTO getCart(Long cartId);

    CartDTO updateProductQuantityInCart(Long productId, Integer delta);

    APIResponse deleteProductFromCart(Long cartId, Long productId);
}
