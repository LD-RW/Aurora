package com.ecommerce.aurora.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
@ToString
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long productId;

    @NotBlank
    @Size(min = 3, message = "Product Name must be at least 3 characters")
    private String productName;
    @NotBlank
    @Size(min = 6, message = "Product Description must be at least 6 characters")
    private String description;
    private Integer quantity;
    private String image;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal specialPrice;

    @SuppressWarnings("JpaDataSourceORMInspection")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User user;
}
