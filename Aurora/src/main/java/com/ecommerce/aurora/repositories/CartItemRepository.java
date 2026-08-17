package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.cartId = ?1 AND ci.product.productId = ?2")
    CartItem findCartItemByProductIdAndCartId(Long cartId, Long productId);

    /**
     * Bulk delete, bypassing the persistence context entirely -- unlike repository.delete(entity),
     * this needs no CartItem loaded first and doesn't rely on Cart.items' orphanRemoval cascade.
     * Not currently called: for a single already-loaded cart, removing the item from
     * cart.getItems() (orphanRemoval = true) already deletes the row on flush, making this
     * redundant there. Kept for the cross-cart cleanup in #40 (a product changing/deleting needs
     * to remove it from every cart that holds it, without loading each Cart's full entity graph
     * just to trigger a cascade).
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = ?1 AND ci.product.productId = ?2")
    void deleteByCartIdAndProductId(Long cartId, Long productId);
}

