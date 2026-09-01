package com.ecommerce.aurora.service;

import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@Profile("!mysql")
@RequiredArgsConstructor
public class SubstringProductSearchStrategy implements ProductSearchStrategy {
    private final ProductRepository productRepository;

    @Override
    public Page<Product> search(String keyword, Pageable pageable) {
        return productRepository.findByProductNameLikeIgnoreCase("%" + keyword + "%", pageable);
    }
}
