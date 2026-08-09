package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
