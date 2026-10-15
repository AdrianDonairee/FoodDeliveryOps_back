package com.adriandonaire.fooddeliveryops.rider;

import com.adriandonaire.fooddeliveryops.auth.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "rider_statuses")
public class RiderStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false, unique = true)
    private AppUser rider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DutyStatus dutyStatus = DutyStatus.NO_TRABAJANDO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.LIBRE;

    @Column
    private Double currentLat;

    @Column
    private Double currentLng;

    @Column
    private Instant lastLocationUpdatedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RiderStatus() {
    }

    public RiderStatus(AppUser rider) {
        this.rider = rider;
        this.dutyStatus = DutyStatus.NO_TRABAJANDO;
        this.availabilityStatus = AvailabilityStatus.LIBRE;
    }

    @PrePersist
    public void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.dutyStatus == null) {
            this.dutyStatus = DutyStatus.NO_TRABAJANDO;
        }
        if (this.availabilityStatus == null) {
            this.availabilityStatus = AvailabilityStatus.LIBRE;
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getRider() {
        return rider;
    }

    public void setRider(AppUser rider) {
        this.rider = rider;
    }

    public DutyStatus getDutyStatus() {
        return dutyStatus;
    }

    public void setDutyStatus(DutyStatus dutyStatus) {
        this.dutyStatus = dutyStatus;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public Double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(Double currentLat) {
        this.currentLat = currentLat;
    }

    public Double getCurrentLng() {
        return currentLng;
    }

    public void setCurrentLng(Double currentLng) {
        this.currentLng = currentLng;
    }

    public Instant getLastLocationUpdatedAt() {
        return lastLocationUpdatedAt;
    }

    public void setLastLocationUpdatedAt(Instant lastLocationUpdatedAt) {
        this.lastLocationUpdatedAt = lastLocationUpdatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
