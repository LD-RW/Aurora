package com.ecommerce.aurora.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO {

    private Long cartItemId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CartDTO cart;

    private ProductDTO product;

    private Integer quantity;

    private BigDecimal productPrice;

    private BigDecimal discount;

}
