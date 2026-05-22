package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);
    
    List<Product> findByProductNameLikeIgnoreCase(String keyword);
}
