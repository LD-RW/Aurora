package com.ecommerce.aurora.mapper;

import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.payload.CategoryDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDTO categoryToCategoryDTO(Category category);
    Category categoryDTOToCategory(CategoryDTO categoryDTO);
}

