package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {

    String WITH_ITEMS = "SELECT DISTINCT c FROM Cart c "
            + "LEFT JOIN FETCH c.items i "
            + "LEFT JOIN FETCH i.product p "
            + "LEFT JOIN FETCH p.category ";

    @Query(WITH_ITEMS + "WHERE c.user.email = ?1")
    Cart findCartByEmail(String email);

    @Query(WITH_ITEMS + "WHERE c.user.email = ?1 AND c.cartId = ?2")
    Cart findCartByEmailAndCartId(String email, Long cartId);

    @Query(WITH_ITEMS)
    List<Cart> findAllWithItems();
}
