package com.ecommerce.aurora.payload;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    @NotBlank
    @Size(min = 3, message = "Product Name must be at least 3 characters")
    private String productName;
    private Long productId;

    @NotBlank
    @Size(min = 6, message = "Product Description must be at least 6 characters")
    private String description;
    private String categoryName;
    private Long categoryId;
    private Integer quantity;
    private String image;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal specialPrice;
}
