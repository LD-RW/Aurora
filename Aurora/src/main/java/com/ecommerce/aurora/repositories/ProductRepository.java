package com.ecommerce.aurora.repositories;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ecommerce.aurora.model.Category;
import com.ecommerce.aurora.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategory(Category category, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByProductNameLikeIgnoreCase(String keyword, Pageable pageable);

    Product findByProductName(String name);

    @Query(value = "SELECT * FROM products "
            + "WHERE MATCH(product_name, description) AGAINST (:keyword IN BOOLEAN MODE) "
            + "ORDER BY MATCH(product_name, description) AGAINST (:keyword IN BOOLEAN MODE) DESC",
            countQuery = "SELECT COUNT(*) FROM products "
                    + "WHERE MATCH(product_name, description) AGAINST (:keyword IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Product> searchByFullText(@Param("keyword") String keyword, Pageable pageable);
}
