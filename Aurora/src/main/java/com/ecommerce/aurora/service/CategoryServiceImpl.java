package com.ecommerce.aurora.service;

import com.ecommerce.aurora.exceptions.APIException;
import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CategoryServiceImpl implements CategoryService{

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        if(categories.isEmpty()){
            throw new APIException("No categories were created till now.");
        }
        return categories;
    }

    @Override
    public void createCategory(Category category) {
        Category createdCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if(createdCategory != null){
            throw new APIException("Category with name " + category.getCategoryName() + " already exists !!!");
        }
        categoryRepository.save(category);
    }

    @Override
    public String deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        categoryRepository.delete(category);
        return "Category with categoryId: " + categoryId + " deleted successfully !!";
    }

    @Override
    public Category updateCategory(Category category, Long categoryId) {
        Category savedOptionalCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        Category updatedCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if(updatedCategory != null){
            throw new APIException("Category with name " + category.getCategoryName() + " already exists !!!");
        }
        category.setCategoryId(categoryId);
        return categoryRepository.save(category);
    }
}