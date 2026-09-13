package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.constants.AppConstants;
import com.ecommerce.aurora.payload.ProductDTO;
import com.ecommerce.aurora.payload.ProductResponse;
import com.ecommerce.aurora.service.FileServiceImpl;
import com.ecommerce.aurora.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ProductController {


    private final ProductService productService;



    @PostMapping("/admin/categories/{categoryId}/product")
    public ResponseEntity<ProductDTO> addProduct(@Valid @RequestBody ProductDTO productDTO, @PathVariable Long categoryId) {

        ProductDTO savedProductDTO = productService.addProduct(categoryId, productDTO);
        return new ResponseEntity<>(savedProductDTO, HttpStatus.CREATED);
    }

    @GetMapping("/public/products")
    public ResponseEntity<ProductResponse> getAllProducts(
            @RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER)
            @Min(value = AppConstants.MIN_PAGE_NUMBER, message = "pageNumber cannot be negative") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE)
            @Min(value = AppConstants.MIN_PAGE_SIZE, message = "pageSize must be at least 1")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "pageSize cannot exceed 100") Integer pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY) String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        ProductResponse productResponse = productService.getAllProducts(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping("/public/categories/{categoryId}/products")
    public ResponseEntity<ProductResponse> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER)
            @Min(value = AppConstants.MIN_PAGE_NUMBER, message = "pageNumber cannot be negative") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE)
            @Min(value = AppConstants.MIN_PAGE_SIZE, message = "pageSize must be at least 1")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "pageSize cannot exceed 100") Integer pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY) String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        ProductResponse productResponse = productService.searchByCategory(categoryId, pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping("/public/products/search")
    public ResponseEntity<ProductResponse> searchProducts(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER)
            @Min(value = AppConstants.MIN_PAGE_NUMBER, message = "pageNumber cannot be negative") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE)
            @Min(value = AppConstants.MIN_PAGE_SIZE, message = "pageSize must be at least 1")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "pageSize cannot exceed 100") Integer pageSize
    ) {
        ProductResponse productResponse = productService.searchByKeyword(keyword, pageNumber, pageSize);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductDTO productDTO) {

        ProductDTO updatedProductDTO = productService.updateProduct(productId, productDTO);
        return new ResponseEntity<>(updatedProductDTO, HttpStatus.OK);
    }

    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable Long productId) {

        ProductDTO deletedProduct = productService.deleteProduct(productId);
        return new ResponseEntity<>(deletedProduct, HttpStatus.OK);
    }

    @PutMapping("/admin/products/{productId}/image")
    public ResponseEntity<ProductDTO> updateProductImage(@PathVariable Long productId, @RequestParam("Image") MultipartFile image) throws IOException {
        ProductDTO updatedProductDTO = productService.updateProductImage(productId, image);
        return new ResponseEntity<>(updatedProductDTO, HttpStatus.OK);
    }

    /**
     * The content type is derived from the stored extension against a fixed allowlist rather
     * than probed from the file itself. Probing returned whatever the uploaded bytes looked
     * like, so an uploaded HTML file came back as text/html and ran as a script in this
     * application's own origin. Uploads are now restricted to real images (see FileServiceImpl),
     * and this mapping is the second half of that guarantee: anything unrecognized is served as
     * an opaque download instead of something the browser will execute.
     */
    @GetMapping("/public/products/image/{fileName}")
    public ResponseEntity<Resource> getProductImage(@PathVariable String fileName) throws IOException {
        Resource resource = productService.getProductImageResource(fileName);
        MediaType mediaType = FileServiceImpl.mediaTypeForFileName(fileName);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
}
