package com.ecommerce.aurora.service;

import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.payload.ProductDTO;
import com.ecommerce.aurora.payload.ProductResponse;

public interface ProductService {
    ProductDTO addProduct(Long categoryId, Product product);

    ProductResponse getAllProducts();

    ProductResponse searchByCategory(Long categoryId);

    ProductResponse searchByKeyword(String keyword);

    ProductDTO updateProduct(Long productId, Product product);

    ProductDTO deleteProduct(Long productId);
}
