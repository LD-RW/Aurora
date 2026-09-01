CREATE FULLTEXT INDEX ft_products_name_description
    ON products (product_name, description);