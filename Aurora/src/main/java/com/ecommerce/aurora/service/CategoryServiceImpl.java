package com.ecommerce.aurora.service;

import com.ecommerce.aurora.constants.AppConstants;
import com.ecommerce.aurora.exceptions.APIException;
import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.mapper.CategoryMapper;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.payload.CategoryDTO;
import com.ecommerce.aurora.payload.CategoryResponse;
import com.ecommerce.aurora.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService{

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize,  String sortBy, String sortOrder) {

        if (!AppConstants.ALLOWED_CATEGORY_SORT_FIELDS.contains(sortBy)) {
            throw new APIException("Invalid sort field: " + sortBy);
        }
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortByAndOrder = Sort.by(direction, sortBy);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
        List<Category> categories = categoryPage.getContent();
        if(categories.isEmpty()){
            throw new APIException("No categories were created till now.");
        }
        List<CategoryDTO> categoryDTOS = categories.stream()
                .map(categoryMapper::categoryToCategoryDTO)
                .toList();
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setTotalElements(categoryPage.getTotalElements());
        categoryResponse.setLastPage(categoryPage.isLast());
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = categoryMapper.categoryDTOToCategory(categoryDTO);
        Category createdCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if(createdCategory != null){
            throw new APIException("Category with name " + category.getCategoryName() + " already exists !!!");
        }
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.categoryToCategoryDTO(savedCategory);

    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        categoryRepository.delete(category);
        return categoryMapper.categoryToCategoryDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
        Category category = categoryMapper.categoryDTOToCategory(categoryDTO);
        Category savedOptionalCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        Category updatedCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if(updatedCategory != null){
            throw new APIException("Category with name " + category.getCategoryName() + " already exists !!!");
        }
        category.setCategoryId(categoryId);
        categoryRepository.save(category);
        return categoryMapper.categoryToCategoryDTO(savedOptionalCategory);
    }
}