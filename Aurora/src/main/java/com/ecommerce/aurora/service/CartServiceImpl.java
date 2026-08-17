package com.ecommerce.aurora.service;

import com.ecommerce.aurora.exceptions.APIException;
import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.mapper.CartMapper;
import com.ecommerce.aurora.model.Cart;
import com.ecommerce.aurora.model.CartItem;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.payload.APIResponse;
import com.ecommerce.aurora.payload.CartDTO;
import com.ecommerce.aurora.repositories.CartItemRepository;
import com.ecommerce.aurora.repositories.CartRepository;
import com.ecommerce.aurora.repositories.ProductRepository;
import com.ecommerce.aurora.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final AuthUtil authUtil;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        Cart cart = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        CartItem newCartItem = new CartItem();

        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItem);
        cart.getItems().add(newCartItem);

        cart.setTotalPrice(cart.getTotalPrice().add(product.getSpecialPrice().multiply(BigDecimal.valueOf(quantity))));

        cartRepository.save(cart);

        return cartMapper.cartToCartDTO(cart);
    }

    @Override
    public List<CartDTO> getAllCarts() {
        return cartRepository.findAllWithItems().stream()
                .map(cartMapper::cartToCartDTO)
                .toList();
    }

    @Override
    public CartDTO getCart(Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(authUtil.loggedInEmail(), cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }
        return cartMapper.cartToCartDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long productId, Integer delta) {
        String email = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(email);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "user", email);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (cartItem == null) {
            throw new ResourceNotFoundException("CartItem", "productId", productId);
        }

        int newQuantity = cartItem.getQuantity() + delta;

        if (newQuantity > product.getQuantity()) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        if (newQuantity <= 0) {
            removeCartItem(cart, cartItem);
        } else {
            BigDecimal oldLineTotal = cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            BigDecimal newLineTotal = product.getSpecialPrice().multiply(BigDecimal.valueOf(newQuantity));
            cart.setTotalPrice(cart.getTotalPrice().subtract(oldLineTotal).add(newLineTotal));

            cartItem.setQuantity(newQuantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepository.save(cartItem);
        }

        cartRepository.save(cart);

        return cartMapper.cartToCartDTO(cart);
    }

    @Override
    @Transactional
    public APIResponse deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(authUtil.loggedInEmail(), cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if (cartItem == null) {
            throw new ResourceNotFoundException("CartItem", "productId", productId);
        }

        String productName = cartItem.getProduct().getProductName();

        removeCartItem(cart, cartItem);
        cartRepository.save(cart);

        return new APIResponse("Product " + productName + " removed from the cart", true);
    }

    /**
     * Subtracts the item's existing (not refreshed) price x quantity contribution from the
     * cart's total and drops it from the in-memory items collection -- shared by the
     * decrease-to-zero branch of updateProductQuantityInCart and the explicit delete endpoint.
     *
     * Removing from cart.getItems() is enough to delete the row: Cart.items is mapped with
     * orphanRemoval = true, so Hibernate schedules its own DELETE for the orphaned CartItem at
     * the next flush. CartItemRepository.deleteByCartIdAndProductId is deliberately NOT called
     * here -- confirmed via SQL logging that Hibernate's auto-flush (triggered because a
     * @Modifying query must synchronize pending state first) runs the orphan-removal DELETE
     * before the explicit query would even execute, making the explicit call a wasted
     * round trip that always matches zero rows for this already-loaded, single-cart case.
     * That query stays on the repository for #40's cross-cart cleanup, which deletes rows
     * for a product across every cart that holds it without loading each Cart's full entity
     * graph first -- a case orphanRemoval can't reach, since nothing pulls those Carts into
     * the persistence context to have items removed from in the first place.
     */
    private void removeCartItem(Cart cart, CartItem cartItem) {
        BigDecimal lineTotal = cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        cart.setTotalPrice(cart.getTotalPrice().subtract(lineTotal));
        cart.getItems().remove(cartItem);
    }

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null) {
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setUser(authUtil.loggedInUser());

        return cartRepository.save(cart);
    }
}
