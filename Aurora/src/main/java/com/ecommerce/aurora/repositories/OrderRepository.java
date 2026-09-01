package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "address", "payment"})
    Page<Order> findAll(Pageable pageable);

    /**
     * Single-order lookup, so unlike findAll(Pageable) above there's no Pageable to
     * collide with a collection JOIN FETCH -- every relation the response needs can
     * be pulled eagerly in one query.
     */
    @Query("SELECT o FROM Order o "
            + "LEFT JOIN FETCH o.user "
            + "LEFT JOIN FETCH o.address "
            + "LEFT JOIN FETCH o.payment "
            + "LEFT JOIN FETCH o.orderItems oi "
            + "LEFT JOIN FETCH oi.product p "
            + "LEFT JOIN FETCH p.category "
            + "WHERE o.orderId = ?1")
    Optional<Order> findByIdWithDetails(Long orderId);

    /**
     * A JOIN FETCH across a to-many collection (orderItems) can't be combined with
     * Pageable directly -- Hibernate would have to apply the LIMIT/OFFSET in memory
     * after fetching every matching row, defeating the point of paginating at all.
     * Instead, findAll(Pageable) above determines which orders belong on the page
     * (to-one relations only, no collection), and this second query -- an ID lookup,
     * not a paginated one -- eagerly loads orderItems (and each item's product/
     * category) for exactly that already-decided set of orders. Both queries run in
     * the same persistence context, so this one populates orderItems directly onto
     * the same Order instances findAll(Pageable) already returned.
     */
    @Query("SELECT DISTINCT o FROM Order o "
            + "LEFT JOIN FETCH o.orderItems oi "
            + "LEFT JOIN FETCH oi.product p "
            + "LEFT JOIN FETCH p.category "
            + "WHERE o.orderId IN ?1")
    List<Order> findAllWithItemsByOrderIdIn(List<Long> orderIds);

    /**
     * Same rationale as findByIdWithDetails -- no Pageable involved here either, so
     * every relation is pulled eagerly in one query instead of relying on N+1 lazy
     * loads through open-in-view while the response is being serialized.
     */
    @Query("SELECT DISTINCT o FROM Order o "
            + "LEFT JOIN FETCH o.user "
            + "LEFT JOIN FETCH o.address "
            + "LEFT JOIN FETCH o.payment "
            + "LEFT JOIN FETCH o.orderItems oi "
            + "LEFT JOIN FETCH oi.product p "
            + "LEFT JOIN FETCH p.category "
            + "WHERE o.user.email = ?1 "
            + "ORDER BY o.orderId")
    List<Order> findByUserEmailWithDetails(String email);
}
