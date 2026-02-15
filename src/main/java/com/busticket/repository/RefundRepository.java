package com.busticket.repository;

import com.busticket.model.Refund;
import com.busticket.model.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, String> {
    
    /**
     * Find refunds by payment ID.
     * 
     * @param paymentId the payment ID
     * @return a list of refunds for the payment
     */
    List<Refund> findByPaymentIdOrderByCreatedAtDesc(String paymentId);
    
    /**
     * Find refunds by booking ID.
     * 
     * @param bookingId the booking ID
     * @return a list of refunds for the booking
     */
    List<Refund> findByBookingIdOrderByCreatedAtDesc(String bookingId);
    
    /**
     * Find refunds by status.
     * 
     * @param status the refund status
     * @return a list of refunds with the specified status
     */
    List<Refund> findByStatusOrderByCreatedAtDesc(RefundStatus status);
    
    /**
     * Find refunds by transaction ID.
     * 
     * @param transactionId the transaction ID
     * @return an Optional containing the refund if found
     */
    Optional<Refund> findByTransactionId(String transactionId);
    
    /**
     * Find refunds created within a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return a list of refunds created within the range
     */
    List<Refund> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Count refunds by status.
     * 
     * @param status the refund status
     * @return the number of refunds with the specified status
     */
    long countByStatus(RefundStatus status);
    
    /**
     * Calculate total refund amount by status within a date range.
     * 
     * @param status the refund status
     * @param startDate the start date
     * @param endDate the end date
     * @return the total amount of refunds with the specified status
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r " +
           "WHERE r.status = :status " +
           "AND r.createdAt BETWEEN :startDate AND :endDate")
    Double calculateRefundTotal(@Param("status") RefundStatus status,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find pending refunds older than specified time.
     * 
     * @param cutoffTime refunds created before this time
     * @return a list of pending refunds that may need attention
     */
    @Query("SELECT r FROM Refund r " +
           "WHERE r.status = 'PENDING' " +
           "AND r.createdAt < :cutoffTime " +
           "ORDER BY r.createdAt ASC")
    List<Refund> findStaleRefunds(@Param("cutoffTime") LocalDateTime cutoffTime);
}