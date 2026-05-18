package com.ecommerce.aurora.mapper;


import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.payload.ProductDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductDTO productToProductDTO(Product product);
    Product productDTOToProduct(ProductDTO productDTO);
}
