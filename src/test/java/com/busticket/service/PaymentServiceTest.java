package com.busticket.service;

import com.busticket.dto.PaymentRequest;
import com.busticket.model.*;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.PaymentRepository;
import com.busticket.repository.RefundRepository;
import com.busticket.service.PaymentService.PaymentSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private SeatLockManager seatLockManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PaymentService paymentService;

    private Booking mockBooking;
    private Payment mockPayment;
    private PaymentRequest mockPaymentRequest;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        mockBooking = new Booking();
        mockBooking.setId("booking-1");
        mockBooking.setPnr("ABC1234567");
        mockBooking.setTripId("trip-1");
        mockBooking.setUserId("user-1");
        mockBooking.setTotalAmount(new BigDecimal("1230.00"));
        mockBooking.setStatus(BookingStatus.PENDING);

        mockPayment = new Payment();
        mockPayment.setId("payment-1");
        mockPayment.setBookingId("booking-1");
        mockPayment.setAmount(new BigDecimal("1230.00"));
        mockPayment.setStatus(PaymentStatus.PENDING);
        mockPayment.setMethod("CREDIT_CARD");
        mockPayment.setCreatedAt(LocalDateTime.now());

        mockPaymentRequest = new PaymentRequest();
        mockPaymentRequest.setMethod("CREDIT_CARD");
        mockPaymentRequest.setCardNumber("4111111111111111");
        mockPaymentRequest.setExpiryDate("12/25");
        mockPaymentRequest.setCvv("123");
    }

    @Test
    void initiatePayment_ShouldSucceedWithValidBooking() {
        // Given
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(mockBooking));
        when(paymentRepository.existsByBookingIdAndStatus("booking-1", PaymentStatus.SUCCESS)).thenReturn(false);

        // When
        PaymentSession session = paymentService.initiatePayment("booking-1", new BigDecimal("1230.00"));

        // Then
        assertNotNull(session);
        assertEquals("booking-1", session.getBookingId());
        assertEquals(new BigDecimal("1230.00"), session.getAmount());
        assertNotNull(session.getSessionId());
        assertNotNull(session.getExpiresAt());
        assertTrue(session.getExpiresAt().isAfter(LocalDateTime.now()));

        verify(valueOperations).set(
            eq("payment_session:" + session.getSessionId()),
            eq(session),
            eq(15L),
            eq(TimeUnit.MINUTES)
        );
    }

    @Test
    void initiatePayment_ShouldFailWithNonExistentBooking() {
        // Given
        when(bookingRepository.findById("invalid-booking")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.initiatePayment("invalid-booking", new BigDecimal("1230.00"));
        });

        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void initiatePayment_ShouldFailWithAlreadyProcessedPayment() {
        // Given
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(mockBooking));
        when(paymentRepository.existsByBookingIdAndStatus("booking-1", PaymentStatus.SUCCESS)).thenReturn(true);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            paymentService.initiatePayment("booking-1", new BigDecimal("1230.00"));
        });

        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void initiatePayment_ShouldFailWithAmountMismatch() {
        // Given
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(mockBooking));
        when(paymentRepository.existsByBookingIdAndStatus("booking-1", PaymentStatus.SUCCESS)).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.initiatePayment("booking-1", new BigDecimal("1000.00")); // Wrong amount
        });

        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void processPayment_ShouldSucceedWithValidSession() {
        // Given
        PaymentSession session = new PaymentSession("session-1", "booking-1", new BigDecimal("1230.00"), LocalDateTime.now().plusMinutes(15));
        when(valueOperations.get("payment_session:session-1")).thenReturn(session);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        
        // Mock successful booking confirmation
        doNothing().when(bookingService).confirmBooking("booking-1", "payment-1");

        // When
        Payment result = paymentService.processPayment("session-1", mockPaymentRequest);

        // Then
        assertNotNull(result);
        assertEquals("payment-1", result.getId());
        assertEquals("booking-1", result.getBookingId());
        assertEquals(new BigDecimal("1230.00"), result.getAmount());
        assertEquals("CREDIT_CARD", result.getMethod());

        verify(paymentRepository, times(2)).save(any(Payment.class)); // Once for creation, once for update
        verify(bookingService).confirmBooking("booking-1", "payment-1");
        verify(redisTemplate).delete("payment_session:session-1");
    }

    @Test
    void processPayment_ShouldFailWithExpiredSession() {
        // Given
        when(valueOperations.get("payment_session:invalid-session")).thenReturn(null);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            paymentService.processPayment("invalid-session", mockPaymentRequest);
        });

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingService, never()).confirmBooking(anyString(), anyString());
    }

    @Test
    void getPayment_ShouldReturnPaymentWhenExists() {
        // Given
        when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(mockPayment));

        // When
        Payment result = paymentService.getPayment("payment-1");

        // Then
        assertNotNull(result);
        assertEquals("payment-1", result.getId());
        assertEquals("booking-1", result.getBookingId());
        assertEquals(new BigDecimal("1230.00"), result.getAmount());
    }

    @Test
    void getPayment_ShouldThrowExceptionWhenNotFound() {
        // Given
        when(paymentRepository.findById("invalid-payment")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.getPayment("invalid-payment");
        });
    }

    @Test
    void getPaymentByBookingId_ShouldReturnMostRecentPayment() {
        // Given
        when(paymentRepository.findTopByBookingIdOrderByCreatedAtDesc("booking-1"))
                .thenReturn(Optional.of(mockPayment));

        // When
        Optional<Payment> result = paymentService.getPaymentByBookingId("booking-1");

        // Then
        assertTrue(result.isPresent());
        assertEquals("payment-1", result.get().getId());
        assertEquals("booking-1", result.get().getBookingId());
    }

    @Test
    void refundPayment_ShouldSucceedWithValidPayment() {
        // Given
        Payment successfulPayment = new Payment();
        successfulPayment.setId("payment-1");
        successfulPayment.setBookingId("booking-1");
        successfulPayment.setAmount(new BigDecimal("1230.00"));
        successfulPayment.setStatus(PaymentStatus.SUCCESS);

        Refund mockRefund = new Refund();
        mockRefund.setId("refund-1");
        mockRefund.setPaymentId("payment-1");
        mockRefund.setBookingId("booking-1");
        mockRefund.setAmount(new BigDecimal("1230.00"));
        mockRefund.setStatus(RefundStatus.PENDING);

        when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(successfulPayment));
        when(refundRepository.save(any(Refund.class))).thenReturn(mockRefund);

        // When
        Refund result = paymentService.refundPayment("payment-1", new BigDecimal("1230.00"), "Customer cancellation");

        // Then
        assertNotNull(result);
        assertEquals("refund-1", result.getId());
        assertEquals("payment-1", result.getPaymentId());
        assertEquals("booking-1", result.getBookingId());
        assertEquals(new BigDecimal("1230.00"), result.getAmount());

        verify(refundRepository, times(2)).save(any(Refund.class)); // Once for creation, once for update
    }

    @Test
    void refundPayment_ShouldFailWithNonSuccessfulPayment() {
        // Given
        Payment failedPayment = new Payment();
        failedPayment.setId("payment-1");
        failedPayment.setStatus(PaymentStatus.FAILED);

        when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(failedPayment));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            paymentService.refundPayment("payment-1", new BigDecimal("1230.00"), "Customer cancellation");
        });

        verify(refundRepository, never()).save(any(Refund.class));
    }

    @Test
    void refundPayment_ShouldFailWithExcessiveRefundAmount() {
        // Given
        Payment successfulPayment = new Payment();
        successfulPayment.setId("payment-1");
        successfulPayment.setAmount(new BigDecimal("1230.00"));
        successfulPayment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(successfulPayment));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.refundPayment("payment-1", new BigDecimal("1500.00"), "Customer cancellation"); // Exceeds payment amount
        });

        verify(refundRepository, never()).save(any(Refund.class));
    }
}