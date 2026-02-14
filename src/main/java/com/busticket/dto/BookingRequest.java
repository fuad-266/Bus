package com.busticket.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BookingRequest {
    
    @NotNull(message = "Trip ID is required")
    private String tripId;
    
    @NotEmpty(message = "At least one seat must be selected")
    private List<String> seatNumbers;
    
    @NotEmpty(message = "At least one passenger is required")
    @Size(min = 1, message = "At least one passenger is required")
    private List<PassengerInfo> passengers;
    
    private String userId;
    
    @NotNull(message = "Lock ID is required")
    private String lockId;

    public BookingRequest() {
    }

    public BookingRequest(String tripId, List<String> seatNumbers, List<PassengerInfo> passengers, String lockId) {
        this.tripId = tripId;
        this.seatNumbers = seatNumbers;
        this.passengers = passengers;
        this.lockId = lockId;
    }

    // Getters and Setters
    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(List<String> seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public List<PassengerInfo> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<PassengerInfo> passengers) {
        this.passengers = passengers;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }
}
