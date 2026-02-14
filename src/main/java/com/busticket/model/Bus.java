package com.busticket.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "buses")
public class Bus {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;
    
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "bus_number", nullable = false, unique = true, length = 50)
    private String busNumber;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "bus_type", nullable = false, length = 20)
    private BusType busType;
    
    @NotNull
    @Min(1)
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;
    
    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "seat_layout", nullable = false, columnDefinition = "jsonb")
    private String seatLayout;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String amenities;
    
    @NotNull
    @Column(name = "admin_user_id", nullable = false, length = 36)
    private String adminUserId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Bus() {
    }

    public Bus(String id, String companyName, String busNumber, BusType busType, Integer totalSeats, String seatLayout, String adminUserId) {
        this.id = id;
        this.companyName = companyName;
        this.busNumber = busNumber;
        this.busType = busType;
        this.totalSeats = totalSeats;
        this.seatLayout = seatLayout;
        this.adminUserId = adminUserId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public BusType getBusType() {
        return busType;
    }

    public void setBusType(BusType busType) {
        this.busType = busType;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getSeatLayout() {
        return seatLayout;
    }

    public void setSeatLayout(String seatLayout) {
        this.seatLayout = seatLayout;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(String adminUserId) {
        this.adminUserId = adminUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
