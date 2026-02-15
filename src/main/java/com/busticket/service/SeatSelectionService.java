package com.busticket.service;

import com.busticket.model.Bus;
import com.busticket.model.Trip;
import com.busticket.repository.BusRepository;
import com.busticket.repository.TripRepository;
import com.busticket.service.SeatLockManager.LockInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SeatSelectionService {

    private final BusRepository busRepository;
    private final TripRepository tripRepository;
    private final SeatLockManager seatLockManager;
    private final ObjectMapper objectMapper;

    public SeatSelectionService(BusRepository busRepository,
                               TripRepository tripRepository,
                               SeatLockManager seatLockManager,
                               ObjectMapper objectMapper) {
        this.busRepository = busRepository;
        this.tripRepository = tripRepository;
        this.seatLockManager = seatLockManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Get the seat layout for a trip with current seat statuses.
     * 
     * @param tripId the trip ID
     * @return SeatLayout with current seat statuses
     */
    public SeatLayout getSeatLayout(String tripId) {
        // Get trip details
        Optional<Trip> trip = tripRepository.findById(tripId);
        if (trip.isEmpty()) {
            throw new IllegalArgumentException("Trip not found: " + tripId);
        }

        // Get bus details
        Optional<Bus> bus = busRepository.findById(trip.get().getBusId());
        if (bus.isEmpty()) {
            throw new IllegalArgumentException("Bus not found for trip: " + tripId);
        }

        // Parse seat layout from bus configuration
        SeatLayoutConfig layoutConfig = parseSeatLayout(bus.get().getSeatLayout());
        
        // Build seat layout with current statuses
        List<Seat> seats = new ArrayList<>();
        for (SeatConfig seatConfig : layoutConfig.getSeats()) {
            SeatStatus status = determineSeatStatus(tripId, seatConfig.getNumber());
            
            Seat seat = new Seat(
                    seatConfig.getNumber(),
                    seatConfig.getRow(),
                    seatConfig.getColumn(),
                    status,
                    trip.get().getPrice()
            );
            seats.add(seat);
        }

        return new SeatLayout(tripId, layoutConfig.getRows(), layoutConfig.getColumns(), seats);
    }

    /**
     * Select a seat and acquire a lock.
     * 
     * @param tripId the trip ID
     * @param seatNumber the seat number to select
     * @param userId the user ID selecting the seat
     * @return SeatSelection result with lock information
     */
    @Transactional
    public SeatSelection selectSeat(String tripId, String seatNumber, String userId) {
        // Check if seat is available
        if (seatLockManager.isLocked(tripId, seatNumber)) {
            throw new IllegalStateException("Seat is already locked: " + seatNumber);
        }

        if (seatLockManager.isBooked(tripId, seatNumber)) {
            throw new IllegalStateException("Seat is already booked: " + seatNumber);
        }

        // Acquire lock for the seat
        LockInfo lockInfo = seatLockManager.acquireLock(tripId, Arrays.asList(seatNumber), userId);
        if (lockInfo == null) {
            throw new IllegalStateException("Failed to acquire lock for seat: " + seatNumber);
        }

        return new SeatSelection(seatNumber, lockInfo.getLockId(), lockInfo.getExpiresAt());
    }

    /**
     * Select multiple seats and acquire locks.
     * 
     * @param tripId the trip ID
     * @param seatNumbers the list of seat numbers to select
     * @param userId the user ID selecting the seats
     * @return SeatSelection result with lock information
     */
    @Transactional
    public SeatSelection selectSeats(String tripId, List<String> seatNumbers, String userId) {
        // Check if all seats are available
        for (String seatNumber : seatNumbers) {
            if (seatLockManager.isLocked(tripId, seatNumber)) {
                throw new IllegalStateException("Seat is already locked: " + seatNumber);
            }

            if (seatLockManager.isBooked(tripId, seatNumber)) {
                throw new IllegalStateException("Seat is already booked: " + seatNumber);
            }
        }

        // Acquire locks for all seats
        LockInfo lockInfo = seatLockManager.acquireLock(tripId, seatNumbers, userId);
        if (lockInfo == null) {
            throw new IllegalStateException("Failed to acquire locks for seats: " + seatNumbers);
        }

        return new SeatSelection(seatNumbers.get(0), lockInfo.getLockId(), lockInfo.getExpiresAt());
    }

    /**
     * Deselect a seat and release its lock.
     * 
     * @param tripId the trip ID
     * @param seatNumber the seat number to deselect
     * @param userId the user ID deselecting the seat
     */
    @Transactional
    public void deselectSeat(String tripId, String seatNumber, String userId) {
        // Find the lock for this seat
        // Note: This is a simplified implementation
        // In a production system, you might need to track user-to-lock mappings
        
        // For now, we'll assume the user knows their lock ID
        // This method signature might need to be updated to include lockId
        throw new UnsupportedOperationException("deselectSeat requires lockId - use deselectSeatByLockId instead");
    }

    /**
     * Deselect seats by releasing the lock.
     * 
     * @param lockId the lock ID to release
     * @return true if lock was released successfully
     */
    @Transactional
    public boolean deselectSeatsByLockId(String lockId) {
        return seatLockManager.releaseLock(lockId);
    }

    /**
     * Calculate the total price for selected seats.
     * 
     * @param tripId the trip ID
     * @param seatNumbers the list of selected seat numbers
     * @return the total price for all selected seats
     */
    public BigDecimal calculateTotalPrice(String tripId, List<String> seatNumbers) {
        Optional<Trip> trip = tripRepository.findById(tripId);
        if (trip.isEmpty()) {
            throw new IllegalArgumentException("Trip not found: " + tripId);
        }

        return trip.get().getPrice().multiply(BigDecimal.valueOf(seatNumbers.size()));
    }

    /**
     * Get fare summary for selected seats.
     * 
     * @param tripId the trip ID
     * @param seatNumbers the list of selected seat numbers
     * @return fare summary with breakdown
     */
    public FareSummary getFareSummary(String tripId, List<String> seatNumbers) {
        Optional<Trip> trip = tripRepository.findById(tripId);
        if (trip.isEmpty()) {
            throw new IllegalArgumentException("Trip not found: " + tripId);
        }

        BigDecimal basePrice = trip.get().getPrice();
        int seatCount = seatNumbers.size();
        
        BigDecimal baseFare = basePrice.multiply(BigDecimal.valueOf(seatCount));
        BigDecimal taxes = baseFare.multiply(BigDecimal.valueOf(0.05)); // 5% tax
        BigDecimal serviceFee = baseFare.multiply(BigDecimal.valueOf(0.02)); // 2% service fee
        BigDecimal totalAmount = baseFare.add(taxes).add(serviceFee);

        return new FareSummary(baseFare, taxes, serviceFee, totalAmount, seatCount);
    }

    /**
     * Determine the current status of a seat.
     * 
     * @param tripId the trip ID
     * @param seatNumber the seat number
     * @return the current seat status
     */
    private SeatStatus determineSeatStatus(String tripId, String seatNumber) {
        if (seatLockManager.isBooked(tripId, seatNumber)) {
            return SeatStatus.BOOKED;
        }
        
        if (seatLockManager.isLocked(tripId, seatNumber)) {
            return SeatStatus.LOCKED;
        }
        
        return SeatStatus.AVAILABLE;
    }

    /**
     * Parse seat layout JSON from bus configuration.
     * 
     * @param seatLayoutJson the JSON string containing seat layout
     * @return parsed seat layout configuration
     */
    private SeatLayoutConfig parseSeatLayout(String seatLayoutJson) {
        if (seatLayoutJson == null || seatLayoutJson.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat layout is empty");
        }

        try {
            return objectMapper.readValue(seatLayoutJson, SeatLayoutConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid seat layout JSON: " + e.getMessage());
        }
    }

    // Data classes

    /**
     * Seat layout for a trip.
     */
    public static class SeatLayout {
        private String tripId;
        private Integer rows;
        private Integer columns;
        private List<Seat> seats;

        public SeatLayout() {
        }

        public SeatLayout(String tripId, Integer rows, Integer columns, List<Seat> seats) {
            this.tripId = tripId;
            this.rows = rows;
            this.columns = columns;
            this.seats = seats;
        }

        // Getters and Setters
        public String getTripId() {
            return tripId;
        }

        public void setTripId(String tripId) {
            this.tripId = tripId;
        }

        public Integer getRows() {
            return rows;
        }

        public void setRows(Integer rows) {
            this.rows = rows;
        }

        public Integer getColumns() {
            return columns;
        }

        public void setColumns(Integer columns) {
            this.columns = columns;
        }

        public List<Seat> getSeats() {
            return seats;
        }

        public void setSeats(List<Seat> seats) {
            this.seats = seats;
        }
    }

    /**
     * Individual seat information.
     */
    public static class Seat {
        private String number;
        private Integer row;
        private Integer column;
        private SeatStatus status;
        private BigDecimal price;

        public Seat() {
        }

        public Seat(String number, Integer row, Integer column, SeatStatus status, BigDecimal price) {
            this.number = number;
            this.row = row;
            this.column = column;
            this.status = status;
            this.price = price;
        }

        // Getters and Setters
        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public Integer getRow() {
            return row;
        }

        public void setRow(Integer row) {
            this.row = row;
        }

        public Integer getColumn() {
            return column;
        }

        public void setColumn(Integer column) {
            this.column = column;
        }

        public SeatStatus getStatus() {
            return status;
        }

        public void setStatus(SeatStatus status) {
            this.status = status;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }

    /**
     * Seat selection result.
     */
    public static class SeatSelection {
        private String seatNumber;
        private String lockId;
        private java.time.LocalDateTime expiresAt;

        public SeatSelection() {
        }

        public SeatSelection(String seatNumber, String lockId, java.time.LocalDateTime expiresAt) {
            this.seatNumber = seatNumber;
            this.lockId = lockId;
            this.expiresAt = expiresAt;
        }

        // Getters and Setters
        public String getSeatNumber() {
            return seatNumber;
        }

        public void setSeatNumber(String seatNumber) {
            this.seatNumber = seatNumber;
        }

        public String getLockId() {
            return lockId;
        }

        public void setLockId(String lockId) {
            this.lockId = lockId;
        }

        public java.time.LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(java.time.LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Fare summary with price breakdown.
     */
    public static class FareSummary {
        private BigDecimal baseFare;
        private BigDecimal taxes;
        private BigDecimal serviceFee;
        private BigDecimal totalAmount;
        private Integer seatCount;

        public FareSummary() {
        }

        public FareSummary(BigDecimal baseFare, BigDecimal taxes, BigDecimal serviceFee, BigDecimal totalAmount, Integer seatCount) {
            this.baseFare = baseFare;
            this.taxes = taxes;
            this.serviceFee = serviceFee;
            this.totalAmount = totalAmount;
            this.seatCount = seatCount;
        }

        // Getters and Setters
        public BigDecimal getBaseFare() {
            return baseFare;
        }

        public void setBaseFare(BigDecimal baseFare) {
            this.baseFare = baseFare;
        }

        public BigDecimal getTaxes() {
            return taxes;
        }

        public void setTaxes(BigDecimal taxes) {
            this.taxes = taxes;
        }

        public BigDecimal getServiceFee() {
            return serviceFee;
        }

        public void setServiceFee(BigDecimal serviceFee) {
            this.serviceFee = serviceFee;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public Integer getSeatCount() {
            return seatCount;
        }

        public void setSeatCount(Integer seatCount) {
            this.seatCount = seatCount;
        }
    }

    /**
     * Seat layout configuration from bus.
     */
    public static class SeatLayoutConfig {
        private Integer rows;
        private Integer columns;
        private List<SeatConfig> seats;

        public SeatLayoutConfig() {
        }

        // Getters and Setters
        public Integer getRows() {
            return rows;
        }

        public void setRows(Integer rows) {
            this.rows = rows;
        }

        public Integer getColumns() {
            return columns;
        }

        public void setColumns(Integer columns) {
            this.columns = columns;
        }

        public List<SeatConfig> getSeats() {
            return seats;
        }

        public void setSeats(List<SeatConfig> seats) {
            this.seats = seats;
        }
    }

    /**
     * Individual seat configuration.
     */
    public static class SeatConfig {
        private String number;
        private Integer row;
        private Integer column;

        public SeatConfig() {
        }

        // Getters and Setters
        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public Integer getRow() {
            return row;
        }

        public void setRow(Integer row) {
            this.row = row;
        }

        public Integer getColumn() {
            return column;
        }

        public void setColumn(Integer column) {
            this.column = column;
        }
    }
}

/**
 * Seat status enumeration.
 */
enum SeatStatus {
    AVAILABLE, SELECTED, BOOKED, LOCKED
}