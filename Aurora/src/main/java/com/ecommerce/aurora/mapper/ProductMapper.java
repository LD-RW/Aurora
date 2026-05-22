package com.ecommerce.aurora.mapper;


import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.payload.ProductDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(source = "category.categoryId", target = "categoryId")
    @Mapping(source = "category.categoryName", target = "categoryName")
    ProductDTO productToProductDTO(Product product);

    @Mapping(source = "categoryId", target = "category.categoryId")
    @Mapping(source = "categoryName", target = "category.categoryName")
    Product productDTOToProduct(ProductDTO productDTO);
}
