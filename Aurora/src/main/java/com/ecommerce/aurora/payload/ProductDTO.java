package com.ecommerce.aurora.payload;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private Long productId;
    private String productName;
    private String image;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal specialPrice;
}
