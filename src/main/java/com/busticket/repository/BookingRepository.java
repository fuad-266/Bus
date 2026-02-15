package com.busticket.repository;

import com.busticket.model.Booking;
import com.busticket.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    
    /**
     * Find a booking by its PNR.
     * 
     * @param pnr the PNR to search for
     * @return an Optional containing the booking if found
     */
    Optional<Booking> findByPnr(String pnr);
    
    /**
     * Find bookings by trip ID and status.
     * 
     * @param tripId the trip ID
     * @param status the booking status
     * @return a list of bookings matching the criteria
     */
    List<Booking> findByTripIdAndStatus(String tripId, BookingStatus status);
    
    /**
     * Find bookings by user ID.
     * 
     * @param userId the user ID
     * @return a list of bookings for the user
     */
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find bookings by user ID and status.
     * 
     * @param userId the user ID
     * @param status the booking status
     * @return a list of bookings matching the criteria
     */
    List<Booking> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, BookingStatus status);
    
    /**
     * Find bookings by trip ID.
     * 
     * @param tripId the trip ID
     * @return a list of bookings for the trip
     */
    List<Booking> findByTripIdOrderByCreatedAtDesc(String tripId);
    
    /**
     * Find bookings by status.
     * 
     * @param status the booking status
     * @return a list of bookings with the specified status
     */
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
    
    /**
     * Find bookings created within a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return a list of bookings created within the range
     */
    List<Booking> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find upcoming bookings for a user (future trips).
     * 
     * @param userId the user ID
     * @param currentTime the current time
     * @return a list of upcoming bookings
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN Trip t ON b.tripId = t.id " +
           "WHERE b.userId = :userId " +
           "AND b.status = 'CONFIRMED' " +
           "AND t.departureTime > :currentTime " +
           "ORDER BY t.departureTime ASC")
    List<Booking> findUpcomingBookingsByUser(@Param("userId") String userId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find past bookings for a user (completed trips).
     * 
     * @param userId the user ID
     * @param currentTime the current time
     * @return a list of past bookings
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN Trip t ON b.tripId = t.id " +
           "WHERE b.userId = :userId " +
           "AND b.status = 'CONFIRMED' " +
           "AND t.departureTime <= :currentTime " +
           "ORDER BY t.departureTime DESC")
    List<Booking> findPastBookingsByUser(@Param("userId") String userId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Check if a PNR exists.
     * 
     * @param pnr the PNR to check
     * @return true if PNR exists, false otherwise
     */
    boolean existsByPnr(String pnr);
    
    /**
     * Count bookings by status.
     * 
     * @param status the booking status
     * @return the number of bookings with the specified status
     */
    long countByStatus(BookingStatus status);
    
    /**
     * Count bookings for a trip.
     * 
     * @param tripId the trip ID
     * @return the number of bookings for the trip
     */
    long countByTripId(String tripId);
    
    /**
     * Count confirmed bookings for a trip.
     * 
     * @param tripId the trip ID
     * @return the number of confirmed bookings for the trip
     */
    long countByTripIdAndStatus(String tripId, BookingStatus status);
    
    /**
     * Find bookings by payment ID.
     * 
     * @param paymentId the payment ID
     * @return an Optional containing the booking if found
     */
    Optional<Booking> findByPaymentId(String paymentId);
    
    /**
     * Find bookings managed by a specific bus admin.
     * 
     * @param adminUserId the admin user ID
     * @return a list of bookings for trips managed by the admin
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN Trip t ON b.tripId = t.id " +
           "JOIN Bus bus ON t.busId = bus.id " +
           "WHERE bus.adminUserId = :adminUserId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findBookingsByBusAdmin(@Param("adminUserId") String adminUserId);
    
    /**
     * Find bookings by bus admin and status.
     * 
     * @param adminUserId the admin user ID
     * @param status the booking status
     * @return a list of bookings matching the criteria
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN Trip t ON b.tripId = t.id " +
           "JOIN Bus bus ON t.busId = bus.id " +
           "WHERE bus.adminUserId = :adminUserId " +
           "AND b.status = :status " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findBookingsByBusAdminAndStatus(@Param("adminUserId") String adminUserId, @Param("status") BookingStatus status);
    
    /**
     * Find bookings by bus admin within a date range.
     * 
     * @param adminUserId the admin user ID
     * @param startDate the start date
     * @param endDate the end date
     * @return a list of bookings within the date range
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN Trip t ON b.tripId = t.id " +
           "JOIN Bus bus ON t.busId = bus.id " +
           "WHERE bus.adminUserId = :adminUserId " +
           "AND t.departureTime BETWEEN :startDate AND :endDate " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findBookingsByBusAdminAndDateRange(
            @Param("adminUserId") String adminUserId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Calculate total revenue for a trip.
     * 
     * @param tripId the trip ID
     * @return the total revenue from confirmed bookings
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
           "WHERE b.tripId = :tripId AND b.status = 'CONFIRMED'")
    Double calculateTripRevenue(@Param("tripId") String tripId);
    
    /**
     * Calculate total revenue for a bus admin.
     * 
     * @param adminUserId the admin user ID
     * @return the total revenue from all trips managed by the admin
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
           "JOIN Trip t ON b.tripId = t.id " +
           "JOIN Bus bus ON t.busId = bus.id " +
           "WHERE bus.adminUserId = :adminUserId " +
           "AND b.status = 'CONFIRMED'")
    Double calculateAdminRevenue(@Param("adminUserId") String adminUserId);
}