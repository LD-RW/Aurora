package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
