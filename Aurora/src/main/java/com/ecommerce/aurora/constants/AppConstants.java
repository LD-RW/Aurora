package com.ecommerce.aurora.constants;

import java.util.List;

public class AppConstants {
    public static final String PAGE_NUMBER = "0";
    public static final String PAGE_SIZE = "10";

    /**
     * Upper bound on pageSize. Without one, a single request could ask for the entire table
     * and load it into memory; PageRequest.of also rejects a size below 1 outright, which
     * surfaced as a 500 until these bounds were enforced at the controller instead.
     */
    public static final int MIN_PAGE_NUMBER = 0;
    public static final int MIN_PAGE_SIZE = 1;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String SORT_CATEGORIES_BY = "categoryId";
    public static final String SORT_PRODUCTS_BY = "productId";
    public static final String SORT_ORDERS_BY = "orderId";
    public static final String SORT_DIR = "asc";

    public static final List<String> ALLOWED_CATEGORY_SORT_FIELDS = List.of("categoryId", "categoryName");
    public static final List<String> ALLOWED_PRODUCT_SORT_FIELDS = List.of("productId", "productName", "price", "specialPrice", "quantity");
    public static final List<String> ALLOWED_ORDER_SORT_FIELDS = List.of("orderId", "orderDate", "totalAmount", "orderStatus");
}
