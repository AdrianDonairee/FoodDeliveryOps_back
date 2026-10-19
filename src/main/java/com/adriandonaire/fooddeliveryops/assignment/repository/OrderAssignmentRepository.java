package com.adriandonaire.fooddeliveryops.assignment.repository;

import com.adriandonaire.fooddeliveryops.assignment.AssignmentStatus;
import com.adriandonaire.fooddeliveryops.assignment.OrderAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {

    @Query("SELECT oa FROM OrderAssignment oa " +
           "JOIN FETCH oa.order o " +
           "LEFT JOIN FETCH o.items " +
           "JOIN FETCH oa.rider r " +
           "WHERE r.id = :riderId AND oa.status = :status AND oa.expiresAt > :now " +
           "ORDER BY oa.offeredAt DESC")
    List<OrderAssignment> findActiveOffersByRiderId(
            @Param("riderId") Long riderId,
            @Param("status") AssignmentStatus status,
            @Param("now") Instant now
    );

    @Query("SELECT oa FROM OrderAssignment oa " +
           "JOIN FETCH oa.order o " +
           "LEFT JOIN FETCH o.items " +
           "JOIN FETCH oa.rider r " +
           "WHERE o.id = :orderId AND r.id = :riderId AND oa.status = :status")
    Optional<OrderAssignment> findByOrderIdAndRiderIdAndStatus(
            @Param("orderId") Long orderId,
            @Param("riderId") Long riderId,
            @Param("status") AssignmentStatus status
    );

    @Query("SELECT oa.rider.id FROM OrderAssignment oa WHERE oa.order.id = :orderId AND oa.status = 'REJECTED'")
    List<Long> findRejectedRiderIdsByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT oa FROM OrderAssignment oa WHERE oa.order.id = :orderId AND oa.status = 'OFFERED'")
    List<OrderAssignment> findActiveOffersByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT COALESCE(SUM(oa.deliveryEarnings), 0.0) FROM OrderAssignment oa WHERE oa.rider.id = :riderId AND oa.status = 'ACCEPTED' AND oa.updatedAt >= :since")
    Double sumEarningsByRiderIdAndSince(
            @Param("riderId") Long riderId,
            @Param("since") Instant since
    );

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM OrderAssignment oa WHERE oa.rider.id = :riderId")
    void deleteAllByRiderId(@Param("riderId") Long riderId);
}
