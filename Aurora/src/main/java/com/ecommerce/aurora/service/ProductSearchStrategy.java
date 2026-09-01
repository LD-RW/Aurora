package com.ecommerce.aurora.service;

import com.ecommerce.aurora.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductSearchStrategy {
    Page<Product> search(String keyword, Pageable pageable);

}
