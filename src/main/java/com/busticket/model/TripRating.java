package com.busticket.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_ratings")
public class TripRating {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @NotNull
    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;
    
    @NotNull
    @Column(name = "bus_id", nullable = false, length = 36)
    private String busId;
    
    @NotNull
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @NotNull
    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5.0")
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;
    
    @Column(columnDefinition = "TEXT")
    private String review;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TripRating() {
    }

    public TripRating(String id, String tripId, String busId, String userId, BigDecimal rating) {
        this.id = id;
        this.tripId = tripId;
        this.busId = busId;
        this.userId = userId;
        this.rating = rating;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
