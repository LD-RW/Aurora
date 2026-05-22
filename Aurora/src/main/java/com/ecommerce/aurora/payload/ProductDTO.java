package com.ecommerce.aurora.payload;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private String productName;
    private Long productId;
    private String description;
    private String categoryName;
    private Long categoryId;
    private Integer quantity;
    private String image;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal specialPrice;
}
