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

    @Override
    @Transactional
    public void syncCartItemsWithProduct(Product product) {
        List<Cart> carts = cartRepository.findAllContainingProduct(product.getProductId());
        for (Cart cart : carts) {
            for (CartItem item : cart.getItems()) {
                if (item.getProduct().getProductId().equals(product.getProductId())) {
                    item.setProductPrice(product.getSpecialPrice());
                    item.setDiscount(product.getDiscount());
                    cartItemRepository.save(item);
                }
            }
            recomputeTotalPrice(cart);
            cartRepository.save(cart);
        }
    }

    @Override
    @Transactional
    public void removeProductFromAllCarts(Long productId) {
        List<Cart> carts = cartRepository.findAllContainingProduct(productId);
        for (Cart cart : carts) {
            CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
            removeCartItem(cart, cartItem);
            cartRepository.save(cart);
        }
    }

    /**
     * Subtracts the item's existing (not refreshed) price x quantity contribution from the
     * cart's total and drops it from the in-memory items collection -- shared by the
     * decrease-to-zero branch of updateProductQuantityInCart, the explicit delete endpoint, and
     * removeProductFromAllCarts.
     *
     * Removing from cart.getItems() is enough to delete the row: Cart.items is mapped with
     * orphanRemoval = true, so Hibernate schedules its own DELETE for the orphaned CartItem at
     * the next flush. Every caller of this method reaches it via a Cart already loaded into the
     * persistence context -- including removeProductFromAllCarts, via
     * CartRepository.findAllContainingProduct -- so orphanRemoval covers every case here.
     * CartItemRepository used to also carry a @Modifying bulk DELETE query as a second removal
     * path, kept specifically for this cross-cart case on the assumption that it would need to
     * delete rows for carts never loaded into memory. It didn't: this method loads every
     * affected cart anyway (to recompute totals / locate the right item), so the bulk query was
     * deleted rather than left as unused, unreachable code.
     */
    private void removeCartItem(Cart cart, CartItem cartItem) {
        BigDecimal lineTotal = cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        cart.setTotalPrice(cart.getTotalPrice().subtract(lineTotal));
        cart.getItems().remove(cartItem);
    }

    /**
     * Sums every line in the cart from scratch, rather than the subtract-old/add-new delta
     * math updateProductQuantityInCart uses. That incremental approach only works cleanly for
     * a single already-known item; here, syncCartItemsWithProduct is iterating carts it just
     * loaded and doesn't have a meaningful "old total" to subtract from other than the one
     * already stored on the entity, so recomputing from the (now-updated) line items is both
     * simpler and self-correcting if the stored total had ever drifted.
     */
    private void recomputeTotalPrice(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalPrice(total);
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
