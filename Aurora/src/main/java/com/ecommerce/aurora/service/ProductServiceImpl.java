package com.ecommerce.aurora.service;


import com.ecommerce.aurora.exceptions.APIException;
import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.mapper.ProductMapper;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.payload.ProductDTO;
import com.ecommerce.aurora.payload.ProductResponse;
import com.ecommerce.aurora.repositories.CategoryRepository;
import com.ecommerce.aurora.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final FileService fileService;
    @Value("${project.image}")
    private String path;
    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        // check if product is already present or not, do it with the help of product name
        Product product = productMapper.productDTOToProduct(productDTO);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        Product existingProduct = productRepository.findByProductName(product.getProductName());
        if (existingProduct != null) {
            throw new APIException("Product with name " + product.getProductName() + " already exists !!!");
        }

        product.setCategory(category);
        product.setImage("default.png");
        product.setSpecialPrice(calculateSpecialPrice(product.getPrice(), product.getDiscount()));
        Product savedProduct = productRepository.save(product);
        return productMapper.productToProductDTO(savedProduct);

    }

    @Override
    public ProductResponse getAllProducts() {

        List<Product> products = productRepository.findAll();
        if(products.isEmpty()) {
            throw new APIException("No Products were created till now");
        }
        List<ProductDTO> productDTOS = products.stream()
                .map(productMapper::productToProductDTO)
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        return productResponse;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        List<Product> products = productRepository.findByCategory(category);
        if (products.isEmpty()) {
            throw new APIException("No products found for this category.");
        }
        List<ProductDTO> productDTOS = products.stream()
                .map(productMapper::productToProductDTO)
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        return productResponse;
    }

    @Override
    public ProductResponse searchByKeyword(String keyword) {
        // check if there is no products added yet then raise an API Exception

        List<Product> products = productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%');
        if (products.isEmpty()) {
            throw new APIException("No products found with keyword: " + keyword);
        }

        List<ProductDTO> productDTOS = products.stream()
                .map(productMapper::productToProductDTO)
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        
        Product product = productMapper.productDTOToProduct(productDTO);
        Product productFromDb =  productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        Product existingProduct = productRepository.findByProductName(product.getProductName());
        if (existingProduct != null && !existingProduct.getProductId().equals(productId)) {
            throw new APIException("Product with name " + product.getProductName() + " already exists !!!");
        }

        productFromDb.setProductName(product.getProductName());
        productFromDb.setDescription(product.getDescription());
        productFromDb.setQuantity(product.getQuantity());
        productFromDb.setPrice(product.getPrice());
        productFromDb.setDiscount(product.getDiscount());
        productFromDb.setSpecialPrice(calculateSpecialPrice(product.getPrice(), product.getDiscount()));
        return productMapper.productToProductDTO(productRepository.save(productFromDb));

    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product productToDelete = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        productRepository.delete(productToDelete);
        return productMapper.productToProductDTO(productToDelete);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        Product productFromDb = productRepository.findById(productId).
                orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        String fileName = fileService.uploadImage(path, image);
        productFromDb.setImage(fileName);
        return productMapper.productToProductDTO(productRepository.save(productFromDb));



    }


    private BigDecimal calculateSpecialPrice(BigDecimal price, BigDecimal discount) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (discount == null || discount.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }

        BigDecimal discountPercentage = discount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal discountValue = price.multiply(discountPercentage);
        BigDecimal specialPrice = price.subtract(discountValue);

        return specialPrice.setScale(2, RoundingMode.HALF_UP);
    }

}
