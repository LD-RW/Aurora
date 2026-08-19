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

    /**
     * Every cart that currently holds the given product, each with its FULL item list loaded
     * (not just the matching item) -- callers need every line to recompute a cart's total
     * price or to locate the specific item to remove.
     *
     * The product filter has to live in a subquery on cartId, not a WHERE directly on the
     * joined-fetched i/p aliases. Filtering a LEFT JOIN FETCH's own join condition would
     * truncate which CartItem rows get fetched to only ones matching the product, which is
     * wrong here -- it would silently drop every OTHER item in an affected cart from
     * cart.getItems(), corrupting any total-price recomputation that iterates it. The subquery
     * only decides which Cart rows are selected; the LEFT JOIN FETCH then pulls in all of each
     * selected cart's items, unfiltered.
     */
    @Query(WITH_ITEMS + "WHERE c.cartId IN (SELECT ci.cart.cartId FROM CartItem ci WHERE ci.product.productId = ?1)")
    List<Cart> findAllContainingProduct(Long productId);
}
