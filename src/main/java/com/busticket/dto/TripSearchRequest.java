package com.busticket.dto;

import com.busticket.model.BusType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TripSearchRequest {
    
    @NotNull(message = "Departure city is required")
    private String departureCity;
    
    @NotNull(message = "Destination city is required")
    private String destinationCity;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalTime departureTimeStart;
    private LocalTime departureTimeEnd;
    private List<BusType> busTypes;
    private Integer minAvailableSeats;
    private List<String> busOperators;
    private String sortBy; // CHEAPEST, FASTEST, EARLIEST_DEPARTURE

    public TripSearchRequest() {
    }

    public TripSearchRequest(String departureCity, String destinationCity, LocalDate date) {
        this.departureCity = departureCity;
        this.destinationCity = destinationCity;
        this.date = date;
    }

    // Getters and Setters
    public String getDepartureCity() {
        return departureCity;
    }

    public void setDepartureCity(String departureCity) {
        this.departureCity = departureCity;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public LocalTime getDepartureTimeStart() {
        return departureTimeStart;
    }

    public void setDepartureTimeStart(LocalTime departureTimeStart) {
        this.departureTimeStart = departureTimeStart;
    }

    public LocalTime getDepartureTimeEnd() {
        return departureTimeEnd;
    }

    public void setDepartureTimeEnd(LocalTime departureTimeEnd) {
        this.departureTimeEnd = departureTimeEnd;
    }

    public List<BusType> getBusTypes() {
        return busTypes;
    }

    public void setBusTypes(List<BusType> busTypes) {
        this.busTypes = busTypes;
    }

    public Integer getMinAvailableSeats() {
        return minAvailableSeats;
    }

    public void setMinAvailableSeats(Integer minAvailableSeats) {
        this.minAvailableSeats = minAvailableSeats;
    }

    public List<String> getBusOperators() {
        return busOperators;
    }

    public void setBusOperators(List<String> busOperators) {
        this.busOperators = busOperators;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
}
