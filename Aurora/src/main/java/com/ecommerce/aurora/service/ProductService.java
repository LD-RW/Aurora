package com.ecommerce.aurora.service;

import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.payload.ProductDTO;
import com.ecommerce.aurora.payload.ProductResponse;

public interface ProductService {
    ProductDTO addProduct(Long categoryId, ProductDTO productDTO);

    ProductResponse getAllProducts();

    ProductResponse searchByCategory(Long categoryId);

    ProductResponse searchByKeyword(String keyword);

    ProductDTO updateProduct(Long productId, ProductDTO productDTO);

    ProductDTO deleteProduct(Long productId);
}
