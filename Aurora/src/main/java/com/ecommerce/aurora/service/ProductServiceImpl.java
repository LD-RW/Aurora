package com.ecommerce.aurora.service;


import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.mapper.CategoryMapper;
import com.ecommerce.aurora.mapper.ProductMapper;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.payload.ProductDTO;
import com.ecommerce.aurora.payload.ProductResponse;
import com.ecommerce.aurora.repositories.CategoryRepository;
import com.ecommerce.aurora.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    @Override
    public ProductDTO addProduct(Long categoryId, Product product) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        product.setCategory(category);
        product.setImage("default.png");
        BigDecimal price = product.getPrice();
        BigDecimal discount = product.getDiscount();
        if (discount == null || discount.compareTo(BigDecimal.ZERO) == 0) {
            product.setSpecialPrice(price);
        } else {
            BigDecimal discountPercentage = discount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal discountValue = price.multiply(discountPercentage);
            BigDecimal specialPrice = price.subtract(discountValue);
            product.setSpecialPrice(specialPrice.setScale(2, RoundingMode.HALF_UP));
        }
        Product savedProduct = productRepository.save(product);
        return productMapper.productToProductDTO(savedProduct);

    }

    @Override
    public ProductResponse getAllProducts() {
        List<Product> products = productRepository.findAll();
        List<ProductDTO> productDTOS = products.stream()
                .map(productMapper::productToProductDTO)
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        return productResponse;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        List<Product> products = productRepository.findByCategory(category);
        List<ProductDTO> productDTOS = products.stream()
                .map(productMapper::productToProductDTO)
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        return productResponse;
    }

}
