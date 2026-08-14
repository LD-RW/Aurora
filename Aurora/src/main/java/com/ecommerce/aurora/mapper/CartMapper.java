package com.ecommerce.aurora.mapper;

import com.ecommerce.aurora.model.Cart;
import com.ecommerce.aurora.model.CartItem;
import com.ecommerce.aurora.payload.CartDTO;
import com.ecommerce.aurora.payload.ProductDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class CartMapper {

    @Autowired
    protected ProductMapper productMapper;

    @Mapping(target = "products", source = "items")
    public abstract CartDTO cartToCartDTO(Cart cart);

    protected ProductDTO cartItemToProductDTO(CartItem cartItem) {
        ProductDTO productDTO = productMapper.productToProductDTO(cartItem.getProduct());
        productDTO.setQuantity(cartItem.getQuantity());
        return productDTO;
    }
}
