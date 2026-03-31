package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
