package com.adriandonaire.fooddeliveryops.order.repository;

import com.adriandonaire.fooddeliveryops.order.Order;
import com.adriandonaire.fooddeliveryops.order.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.foodShop.id = :foodShopId ORDER BY o.createdAt DESC")
    List<Order> findByFoodShopIdWithItemsOrderByCreatedAtDesc(@Param("foodShopId") Long foodShopId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id AND o.foodShop.id = :foodShopId")
    Optional<Order> findByIdAndFoodShopIdWithItems(@Param("id") Long id, @Param("foodShopId") Long foodShopId);

    List<Order> findByStatus(OrderStatus status);
}
