package com.ecommerce.aurora.mapper;

import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.payload.CategoryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDTO categoryToCategoryDTO(Category category);

    @Mapping(target = "categoryId", ignore = true)
    Category categoryDTOToCategory(CategoryDTO categoryDTO);
}

