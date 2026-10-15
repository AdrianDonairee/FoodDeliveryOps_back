package com.adriandonaire.fooddeliveryops.rider.repository;

import com.adriandonaire.fooddeliveryops.rider.AvailabilityStatus;
import com.adriandonaire.fooddeliveryops.rider.DutyStatus;
import com.adriandonaire.fooddeliveryops.rider.RiderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RiderStatusRepository extends JpaRepository<RiderStatus, Long> {

    @Query("SELECT rs FROM RiderStatus rs JOIN FETCH rs.rider r WHERE r.id = :riderId")
    Optional<RiderStatus> findByRiderId(@Param("riderId") Long riderId);

    @Query("SELECT rs FROM RiderStatus rs JOIN FETCH rs.rider r WHERE r.username = :username")
    Optional<RiderStatus> findByRiderUsername(@Param("username") String username);

    @Query("SELECT rs FROM RiderStatus rs JOIN FETCH rs.rider r WHERE rs.dutyStatus = :dutyStatus AND rs.availabilityStatus = :availabilityStatus")
    List<RiderStatus> findByDutyStatusAndAvailabilityStatus(
            @Param("dutyStatus") DutyStatus dutyStatus,
            @Param("availabilityStatus") AvailabilityStatus availabilityStatus
    );
}
