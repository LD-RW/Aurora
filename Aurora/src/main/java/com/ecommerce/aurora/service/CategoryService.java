package com.ecommerce.aurora.service;
import com.ecommerce.aurora.payload.CategoryDTO;
import com.ecommerce.aurora.payload.CategoryResponse;

public interface CategoryService {
    CategoryResponse getAllCategories();
    CategoryDTO createCategory(CategoryDTO categoryDTO);

    CategoryDTO deleteCategory(Long categoryId);

    CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId);
}
