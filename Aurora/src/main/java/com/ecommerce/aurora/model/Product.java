package com.ecommerce.aurora.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    private String productName;
    private String description;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal specialPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;
}
