package com.busticket.service;

import com.busticket.dto.BookingRequest;
import com.busticket.dto.BookingResponse;
import com.busticket.dto.PassengerInfo;
import com.busticket.model.Booking;
import com.busticket.model.BookingStatus;
import com.busticket.model.Trip;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.BusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private static final BigDecimal TAX_RATE = new BigDecimal("0.18"); // 18% tax
    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.05"); // 5% service fee
    private static final String LOCK_PREFIX = "lock:";
    
    private final BookingRepository bookingRepository;
    private final BusRepository busRepository;
    private final SeatLockManager seatLockManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public BookingService(BookingRepository bookingRepository, 
                         BusRepository busRepository,
                         SeatLockManager seatLockManager,
                         RedisTemplate<String, Object> redisTemplate) {
        this.bookingRepository = bookingRepository;
        this.busRepository = busRepository;
        this.seatLockManager = seatLockManager;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Creates a new booking with validation
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        logger.info("Creating booking for trip: {}, seats: {}", request.getTripId(), request.getSeatNumbers());
        
        // Validate lock
        validateLock(request.getLockId(), request.getTripId(), request.getSeatNumbers());
        
        // Validate passenger count
        validatePassengerCount(request.getPassengers(), request.getSeatNumbers());
        
        // Get trip details
        Trip trip = busRepository.findTripById(request.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + request.getTripId()));
        
        // Calculate pricing
        PricingDetails pricing = calculatePricing(trip.getPrice(), request.getSeatNumbers().size());
        
        // Generate PNR
        String pnr = generatePNR();
        
        // Create booking entity
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID().toString());
        booking.setPnr(pnr);
        booking.setTripId(request.getTripId());
        booking.setUserId(request.getUserId());
        booking.setSeatNumbers(String.join(",", request.getSeatNumbers()));
        booking.setPassengers(convertPassengersToJson(request.getPassengers()));
        booking.setTotalAmount(pricing.totalAmount);
        booking.setTaxes(pricing.taxes);
        booking.setServiceFee(pricing.serviceFee);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        
        // Save booking
        booking = bookingRepository.save(booking);
        
        // Store lock-to-booking mapping for later cleanup
        storeLockBookingMapping(request.getLockId(), booking.getId());
        
        logger.info("Booking created successfully: PNR={}, ID={}", pnr, booking.getId());
        
        return convertToBookingResponse(booking, trip);
    }

    /**
     * Confirms a booking after successful payment
     */
    @Transactional
    public BookingResponse confirmBooking(String bookingId, String paymentId) {
        logger.info("Confirming booking: {}, payment: {}", bookingId, paymentId);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking already processed: " + booking.getStatus());
        }
        
        // Update booking status
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);
        booking.setConfirmedAt(LocalDateTime.now());
        
        booking = bookingRepository.save(booking);
        
        // Release seat locks
        releaseLockForBooking(bookingId);
        
        // Get trip details for response
        Trip trip = busRepository.findTripById(booking.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + booking.getTripId()));
        
        logger.info("Booking confirmed successfully: PNR={}", booking.getPnr());
        
        return convertToBookingResponse(booking, trip);
    }

    /**
     * Cancels a booking and releases seats
     */
    @Transactional
    public void cancelBooking(String bookingId, String userId) {
        logger.info("Cancelling booking: {}, user: {}", bookingId, userId);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        // Verify ownership
        if (userId != null && !userId.equals(booking.getUserId())) {
            throw new IllegalArgumentException("User not authorized to cancel this booking");
        }
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking already cancelled");
        }
        
        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        
        bookingRepository.save(booking);
        
        // Release locks if booking was pending
        if (booking.getStatus() == BookingStatus.PENDING) {
            releaseLockForBooking(bookingId);
        }
        
        logger.info("Booking cancelled successfully: PNR={}", booking.getPnr());
    }

    /**
     * Retrieves a booking by ID
     */
    public BookingResponse getBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        Trip trip = busRepository.findTripById(booking.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + booking.getTripId()));
        
        return convertToBookingResponse(booking, trip);
    }

    /**
     * Retrieves all bookings for a user
     */
    public List<BookingResponse> getUserBookings(String userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return bookings.stream()
                .map(booking -> {
                    Trip trip = busRepository.findTripById(booking.getTripId())
                            .orElse(null);
                    return convertToBookingResponse(booking, trip);
                })
                .toList();
    }

    /**
     * Retrieves a booking by PNR
     */
    public BookingResponse getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with PNR: " + pnr));
        
        Trip trip = busRepository.findTripById(booking.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + booking.getTripId()));
        
        return convertToBookingResponse(booking, trip);
    }

    // Private helper methods

    private void validateLock(String lockId, String tripId, List<String> seatNumbers) {
        if (lockId == null || lockId.trim().isEmpty()) {
            throw new IllegalArgumentException("Lock ID is required");
        }
        
        // Check if lock exists and is valid
        Object lockInfo = redisTemplate.opsForValue().get(LOCK_PREFIX + lockId);
        if (lockInfo == null) {
            throw new IllegalStateException("Lock expired or invalid");
        }
        
        // Verify all seats are still locked
        for (String seatNumber : seatNumbers) {
            if (!seatLockManager.isLocked(tripId, seatNumber)) {
                throw new IllegalStateException("Seat " + seatNumber + " is no longer locked");
            }
        }
    }

    private void validatePassengerCount(List<PassengerInfo> passengers, List<String> seatNumbers) {
        if (passengers == null || passengers.isEmpty()) {
            throw new IllegalArgumentException("Passenger information is required");
        }
        
        if (passengers.size() != seatNumbers.size()) {
            throw new IllegalArgumentException(
                String.format("Passenger count (%d) must match seat count (%d)", 
                    passengers.size(), seatNumbers.size()));
        }
        
        // Validate passenger details
        for (int i = 0; i < passengers.size(); i++) {
            PassengerInfo passenger = passengers.get(i);
            if (passenger.getName() == null || passenger.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Passenger " + (i + 1) + " name is required");
            }
            if (passenger.getPhone() == null || passenger.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("Passenger " + (i + 1) + " phone is required");
            }
            if (passenger.getEmail() == null || passenger.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Passenger " + (i + 1) + " email is required");
            }
        }
    }

    private PricingDetails calculatePricing(BigDecimal basePrice, int seatCount) {
        BigDecimal baseFare = basePrice.multiply(new BigDecimal(seatCount));
        BigDecimal taxes = baseFare.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = baseFare.multiply(SERVICE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseFare.add(taxes).add(serviceFee);
        
        return new PricingDetails(baseFare, taxes, serviceFee, totalAmount);
    }

    private String generatePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder pnr = new StringBuilder();
        
        for (int i = 0; i < 10; i++) {
            pnr.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // Ensure uniqueness
        while (bookingRepository.existsByPnr(pnr.toString())) {
            pnr = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                pnr.append(chars.charAt(random.nextInt(chars.length())));
            }
        }
        
        return pnr.toString();
    }

    private String convertPassengersToJson(List<PassengerInfo> passengers) {
        // Simple JSON conversion - in production, use Jackson or similar
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < passengers.size(); i++) {
            PassengerInfo p = passengers.get(i);
            if (i > 0) json.append(",");
            json.append("{")
                .append("\"name\":\"").append(p.getName()).append("\",")
                .append("\"phone\":\"").append(p.getPhone()).append("\",")
                .append("\"email\":\"").append(p.getEmail()).append("\"")
                .append("}");
        }
        json.append("]");
        return json.toString();
    }

    private void storeLockBookingMapping(String lockId, String bookingId) {
        redisTemplate.opsForValue().set("lock_booking:" + lockId, bookingId, 15, TimeUnit.MINUTES);
    }

    private void releaseLockForBooking(String bookingId) {
        try {
            // Find lock ID associated with this booking
            String lockId = findLockIdForBooking(bookingId);
            if (lockId != null) {
                seatLockManager.releaseLock(lockId);
                redisTemplate.delete("lock_booking:" + lockId);
            }
        } catch (Exception e) {
            logger.warn("Failed to release lock for booking {}: {}", bookingId, e.getMessage());
        }
    }

    private String findLockIdForBooking(String bookingId) {
        // This is a simplified approach - in production, you might want to store this mapping more efficiently
        return null; // Implementation would depend on how you store the lock-booking mapping
    }

    private BookingResponse convertToBookingResponse(Booking booking, Trip trip) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setPnr(booking.getPnr());
        response.setTripId(booking.getTripId());
        response.setUserId(booking.getUserId());
        response.setSeatNumbers(List.of(booking.getSeatNumbers().split(",")));
        response.setTotalAmount(booking.getTotalAmount());
        response.setTaxes(booking.getTaxes());
        response.setServiceFee(booking.getServiceFee());
        response.setStatus(booking.getStatus().toString());
        response.setCreatedAt(booking.getCreatedAt());
        response.setConfirmedAt(booking.getConfirmedAt());
        
        if (trip != null) {
            response.setDepartureCity(trip.getDepartureCity());
            response.setDestinationCity(trip.getDestinationCity());
            response.setDepartureTime(trip.getDepartureTime());
            response.setArrivalTime(trip.getArrivalTime());
        }
        
        return response;
    }

    // Inner class for pricing calculations
    private static class PricingDetails {
        final BigDecimal baseFare;
        final BigDecimal taxes;
        final BigDecimal serviceFee;
        final BigDecimal totalAmount;

        PricingDetails(BigDecimal baseFare, BigDecimal taxes, BigDecimal serviceFee, BigDecimal totalAmount) {
            this.baseFare = baseFare;
            this.taxes = taxes;
            this.serviceFee = serviceFee;
            this.totalAmount = totalAmount;
        }
    }
}