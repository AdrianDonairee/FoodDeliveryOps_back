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

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.assignedRider.id = :riderId AND o.status IN :statuses")
    Optional<Order> findActiveOrderByRiderId(
            @Param("riderId") Long riderId,
            @Param("statuses") java.util.Collection<OrderStatus> statuses
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedRider.id = :riderId AND o.status = :status AND o.updatedAt >= :since")
    long countByRiderIdAndStatusAndUpdatedAtAfter(
            @Param("riderId") Long riderId,
            @Param("status") OrderStatus status,
            @Param("since") java.time.Instant since
    );

    boolean existsByFoodShopId(Long foodShopId);

    boolean existsByAssignedRiderId(Long riderId);
}
