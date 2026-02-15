package com.busticket.service;

import com.busticket.dto.PaymentRequest;
import com.busticket.model.Booking;
import com.busticket.model.Payment;
import com.busticket.model.PaymentStatus;
import com.busticket.model.Refund;
import com.busticket.model.RefundStatus;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.PaymentRepository;
import com.busticket.repository.RefundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYMENT_SESSION_PREFIX = "payment_session:";
    private static final int SESSION_TIMEOUT_MINUTES = 15;
    
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final RefundRepository refundRepository;
    private final BookingService bookingService;
    private final SeatLockManager seatLockManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                         BookingRepository bookingRepository,
                         RefundRepository refundRepository,
                         BookingService bookingService,
                         SeatLockManager seatLockManager,
                         RedisTemplate<String, Object> redisTemplate) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.refundRepository = refundRepository;
        this.bookingService = bookingService;
        this.seatLockManager = seatLockManager;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Initiates a payment session for a booking
     */
    public PaymentSession initiatePayment(String bookingId, BigDecimal amount) {
        logger.info("Initiating payment for booking: {}, amount: {}", bookingId, amount);
        
        // Validate booking exists
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        // Check for duplicate successful payment
        if (paymentRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.SUCCESS)) {
            throw new IllegalStateException("Payment already processed for booking: " + bookingId);
        }
        
        // Validate amount matches booking total
        if (amount.compareTo(booking.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match booking total");
        }
        
        // Create payment session
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES);
        
        PaymentSession session = new PaymentSession(sessionId, bookingId, amount, expiresAt);
        
        // Store session in Redis with TTL
        redisTemplate.opsForValue().set(
            PAYMENT_SESSION_PREFIX + sessionId, 
            session, 
            SESSION_TIMEOUT_MINUTES, 
            TimeUnit.MINUTES
        );
        
        logger.info("Payment session created: {}", sessionId);
        return session;
    }

    /**
     * Processes a payment using the provided payment details
     */
    @Transactional
    public Payment processPayment(String sessionId, PaymentRequest paymentRequest) {
        logger.info("Processing payment for session: {}", sessionId);
        
        // Validate payment session
        PaymentSession session = getPaymentSession(sessionId);
        if (session == null) {
            throw new IllegalStateException("Payment session expired or invalid");
        }
        
        // Create payment record
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setBookingId(session.getBookingId());
        payment.setAmount(session.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(paymentRequest.getMethod());
        payment.setCreatedAt(LocalDateTime.now());
        
        payment = paymentRepository.save(payment);
        
        try {
            // Process payment through mock gateway
            PaymentGatewayResult result = processPaymentThroughGateway(payment, paymentRequest);
            
            // Update payment status based on result
            if (result.isSuccess()) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setTransactionId(result.getTransactionId());
                payment.setCompletedAt(LocalDateTime.now());
                
                // Confirm booking
                bookingService.confirmBooking(payment.getBookingId(), payment.getId());
                
                logger.info("Payment successful: {}, transaction: {}", payment.getId(), result.getTransactionId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                
                // Release seat locks for failed payment
                releaseSeatLocksForBooking(payment.getBookingId());
                
                logger.warn("Payment failed: {}, reason: {}", payment.getId(), result.getErrorMessage());
            }
            
            payment = paymentRepository.save(payment);
            
            // Clean up payment session
            redisTemplate.delete(PAYMENT_SESSION_PREFIX + sessionId);
            
            return payment;
            
        } catch (Exception e) {
            logger.error("Payment processing error for payment: {}", payment.getId(), e);
            
            // Mark payment as failed
            payment.setStatus(PaymentStatus.FAILED);
            payment = paymentRepository.save(payment);
            
            // Release seat locks
            releaseSeatLocksForBooking(payment.getBookingId());
            
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a payment by ID
     */
    public Payment getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    /**
     * Retrieves a payment by booking ID
     */
    public Optional<Payment> getPaymentByBookingId(String bookingId) {
        return paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    /**
     * Processes a refund for a payment
     */
    @Transactional
    public Refund refundPayment(String paymentId, BigDecimal refundAmount, String reason) {
        logger.info("Processing refund for payment: {}, amount: {}", paymentId, refundAmount);
        
        Payment payment = getPayment(paymentId);
        
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Cannot refund non-successful payment");
        }
        
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }
        
        // Create refund record
        Refund refund = new Refund();
        refund.setId(UUID.randomUUID().toString());
        refund.setPaymentId(paymentId);
        refund.setBookingId(payment.getBookingId());
        refund.setAmount(refundAmount);
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PENDING);
        refund.setCreatedAt(LocalDateTime.now());
        
        refund = refundRepository.save(refund);
        
        try {
            // Process refund through mock gateway
            RefundGatewayResult result = processRefundThroughGateway(refund, payment);
            
            if (result.isSuccess()) {
                refund.setStatus(RefundStatus.COMPLETED);
                refund.setTransactionId(result.getTransactionId());
                refund.setCompletedAt(LocalDateTime.now());
                
                logger.info("Refund successful: {}, transaction: {}", refund.getId(), result.getTransactionId());
            } else {
                refund.setStatus(RefundStatus.FAILED);
                
                logger.warn("Refund failed: {}, reason: {}", refund.getId(), result.getErrorMessage());
            }
            
            refund = refundRepository.save(refund);
            return refund;
            
        } catch (Exception e) {
            logger.error("Refund processing error for refund: {}", refund.getId(), e);
            
            refund.setStatus(RefundStatus.FAILED);
            refund = refundRepository.save(refund);
            
            throw new RuntimeException("Refund processing failed: " + e.getMessage(), e);
        }
    }

    // Private helper methods

    private PaymentSession getPaymentSession(String sessionId) {
        return (PaymentSession) redisTemplate.opsForValue().get(PAYMENT_SESSION_PREFIX + sessionId);
    }

    private PaymentGatewayResult processPaymentThroughGateway(Payment payment, PaymentRequest request) {
        // Mock payment gateway - simulate payment processing
        logger.info("Processing payment through mock gateway: {}", payment.getId());
        
        // Simulate processing delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock success/failure logic (90% success rate)
        Random random = new Random();
        boolean success = random.nextDouble() < 0.9;
        
        if (success) {
            String transactionId = "TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return new PaymentGatewayResult(true, transactionId, null);
        } else {
            return new PaymentGatewayResult(false, null, "Insufficient funds or card declined");
        }
    }

    private RefundGatewayResult processRefundThroughGateway(Refund refund, Payment originalPayment) {
        // Mock refund gateway - simulate refund processing
        logger.info("Processing refund through mock gateway: {}", refund.getId());
        
        // Simulate processing delay
        try {
            Thread.sleep(500); // 0.5 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock success (95% success rate for refunds)
        Random random = new Random();
        boolean success = random.nextDouble() < 0.95;
        
        if (success) {
            String transactionId = "REF_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return new RefundGatewayResult(true, transactionId, null);
        } else {
            return new RefundGatewayResult(false, null, "Refund processing failed at gateway");
        }
    }

    private void releaseSeatLocksForBooking(String bookingId) {
        try {
            // This is a simplified approach - in production, you'd need a more robust way
            // to track and release locks associated with a booking
            logger.info("Releasing seat locks for failed payment, booking: {}", bookingId);
            // Implementation would depend on how you store the booking-lock relationship
        } catch (Exception e) {
            logger.warn("Failed to release seat locks for booking {}: {}", bookingId, e.getMessage());
        }
    }

    // Inner classes for payment session and gateway results

    public static class PaymentSession {
        private final String sessionId;
        private final String bookingId;
        private final BigDecimal amount;
        private final LocalDateTime expiresAt;

        public PaymentSession(String sessionId, String bookingId, BigDecimal amount, LocalDateTime expiresAt) {
            this.sessionId = sessionId;
            this.bookingId = bookingId;
            this.amount = amount;
            this.expiresAt = expiresAt;
        }

        public String getSessionId() { return sessionId; }
        public String getBookingId() { return bookingId; }
        public BigDecimal getAmount() { return amount; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }

    private static class PaymentGatewayResult {
        private final boolean success;
        private final String transactionId;
        private final String errorMessage;

        public PaymentGatewayResult(boolean success, String transactionId, String errorMessage) {
            this.success = success;
            this.transactionId = transactionId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getTransactionId() { return transactionId; }
        public String getErrorMessage() { return errorMessage; }
    }

    private static class RefundGatewayResult {
        private final boolean success;
        private final String transactionId;
        private final String errorMessage;

        public RefundGatewayResult(boolean success, String transactionId, String errorMessage) {
            this.success = success;
            this.transactionId = transactionId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getTransactionId() { return transactionId; }
        public String getErrorMessage() { return errorMessage; }
    }
}