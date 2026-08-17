package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.exceptions.APIException;
import com.ecommerce.aurora.payload.APIResponse;
import com.ecommerce.aurora.payload.CartDTO;
import com.ecommerce.aurora.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/carts/products/{productId}/quantity/{quantity}")
    public ResponseEntity<CartDTO> addProductToCart(@PathVariable Long productId,
                                                    @PathVariable Integer quantity){
        CartDTO cartDTO = cartService.addProductToCart(productId, quantity);
        return new ResponseEntity<>(cartDTO, HttpStatus.CREATED);
    }

    @GetMapping("/admin/carts")
    public ResponseEntity<List<CartDTO>> getAllCarts(){
        List<CartDTO> carts = cartService.getAllCarts();
        return new ResponseEntity<>(carts, HttpStatus.OK);
    }

    @GetMapping("/carts/{cartId}")
    public ResponseEntity<CartDTO> getCart(@PathVariable Long cartId){
        CartDTO cartDTO = cartService.getCart(cartId);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @PutMapping("/carts/products/{productId}/quantity/{operation}")
    public ResponseEntity<CartDTO> updateProductQuantityInCart(@PathVariable Long productId,
                                                                 @PathVariable String operation){
        Integer delta = switch (operation.toLowerCase()) {
            case "increase" -> 1;
            case "decrease" -> -1;
            default -> throw new APIException("Invalid operation '" + operation + "'. Use 'increase' or 'decrease'.");
        };
        CartDTO cartDTO = cartService.updateProductQuantityInCart(productId, delta);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @DeleteMapping("/carts/{cartId}/products/{productId}")
    public ResponseEntity<APIResponse> deleteProductFromCart(@PathVariable Long cartId,
                                                               @PathVariable Long productId){
        APIResponse response = cartService.deleteProductFromCart(cartId, productId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
