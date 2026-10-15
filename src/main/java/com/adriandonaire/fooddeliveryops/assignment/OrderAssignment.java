package com.adriandonaire.fooddeliveryops.assignment;

import com.adriandonaire.fooddeliveryops.auth.AppUser;
import com.adriandonaire.fooddeliveryops.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "order_assignments")
public class OrderAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rider_id", nullable = false)
    private AppUser rider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssignmentStatus status = AssignmentStatus.OFFERED;

    @Column(nullable = false)
    private Double distanceKm;

    @Column(nullable = false)
    private Double deliveryEarnings;

    @Column(nullable = false)
    private Instant offeredAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant respondedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected OrderAssignment() {
    }

    public OrderAssignment(Order order, AppUser rider, Double distanceKm, Double deliveryEarnings, Instant expiresAt) {
        this.order = order;
        this.rider = rider;
        this.distanceKm = distanceKm;
        this.deliveryEarnings = deliveryEarnings;
        this.offeredAt = Instant.now();
        this.expiresAt = expiresAt;
        this.status = AssignmentStatus.OFFERED;
    }

    @PrePersist
    public void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.offeredAt == null) {
            this.offeredAt = now;
        }
        if (this.status == null) {
            this.status = AssignmentStatus.OFFERED;
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public AppUser getRider() {
        return rider;
    }

    public void setRider(AppUser rider) {
        this.rider = rider;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getDeliveryEarnings() {
        return deliveryEarnings;
    }

    public void setDeliveryEarnings(Double deliveryEarnings) {
        this.deliveryEarnings = deliveryEarnings;
    }

    public Instant getOfferedAt() {
        return offeredAt;
    }

    public void setOfferedAt(Instant offeredAt) {
        this.offeredAt = offeredAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
