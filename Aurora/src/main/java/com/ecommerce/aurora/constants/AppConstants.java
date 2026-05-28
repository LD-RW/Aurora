package com.ecommerce.aurora.constants;

import java.util.List;

public class AppConstants {
    public static final String PAGE_NUMBER = "0";
    public static final String PAGE_SIZE = "10";
    public static final String SORT_CATEGORIES_BY = "categoryId";
    public static final String SORT_PRODUCTS_BY = "productId";
    public static final String SORT_DIR = "asc";

    public static final List<String> ALLOWED_CATEGORY_SORT_FIELDS = List.of("categoryId", "categoryName");
    public static final List<String> ALLOWED_PRODUCT_SORT_FIELDS = List.of("productId", "productName", "price", "specialPrice", "quantity");
}
