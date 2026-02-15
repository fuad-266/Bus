package com.busticket.repository;

import com.busticket.model.Payment;
import com.busticket.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    
    /**
     * Find a payment by booking ID.
     * 
     * @param bookingId the booking ID
     * @return an Optional containing the payment if found
     */
    Optional<Payment> findByBookingId(String bookingId);
    
    /**
     * Find payments by booking ID and status.
     * 
     * @param bookingId the booking ID
     * @param status the payment status
     * @return a list of payments matching the criteria
     */
    List<Payment> findByBookingIdAndStatus(String bookingId, PaymentStatus status);
    
    /**
     * Find payments by status.
     * 
     * @param status the payment status
     * @return a list of payments with the specified status
     */
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    
    /**
     * Find payments by transaction ID.
     * 
     * @param transactionId the transaction ID
     * @return an Optional containing the payment if found
     */
    Optional<Payment> findByTransactionId(String transactionId);
    
    /**
     * Find payments created within a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return a list of payments created within the range
     */
    List<Payment> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Check if a successful payment exists for a booking.
     * 
     * @param bookingId the booking ID
     * @return true if a successful payment exists, false otherwise
     */
    boolean existsByBookingIdAndStatus(String bookingId, PaymentStatus status);
    
    /**
     * Count payments by status.
     * 
     * @param status the payment status
     * @return the number of payments with the specified status
     */
    long countByStatus(PaymentStatus status);
    
    /**
     * Find the most recent payment for a booking.
     * 
     * @param bookingId the booking ID
     * @return an Optional containing the most recent payment if found
     */
    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(String bookingId);
    
    /**
     * Find payments by method.
     * 
     * @param method the payment method
     * @return a list of payments using the specified method
     */
    List<Payment> findByMethodOrderByCreatedAtDesc(String method);
    
    /**
     * Calculate total successful payment amount within a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return the total amount of successful payments
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'SUCCESS' " +
           "AND p.completedAt BETWEEN :startDate AND :endDate")
    Double calculateSuccessfulPaymentsTotal(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find failed payments that can be retried.
     * 
     * @param cutoffTime payments created before this time
     * @return a list of failed payments that can be retried
     */
    @Query("SELECT p FROM Payment p " +
           "WHERE p.status = 'FAILED' " +
           "AND p.createdAt > :cutoffTime " +
           "ORDER BY p.createdAt DESC")
    List<Payment> findRetryableFailedPayments(@Param("cutoffTime") LocalDateTime cutoffTime);
}