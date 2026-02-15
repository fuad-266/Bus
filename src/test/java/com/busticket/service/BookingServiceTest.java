package com.busticket.service;

import com.busticket.dto.BookingRequest;
import com.busticket.dto.BookingResponse;
import com.busticket.dto.PassengerInfo;
import com.busticket.model.Booking;
import com.busticket.model.BookingStatus;
import com.busticket.model.Trip;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.BusRepository;
import com.busticket.repository.TripRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private SeatLockManager seatLockManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private BookingService bookingService;

    private Trip mockTrip;
    private BookingRequest mockBookingRequest;
    private List<PassengerInfo> mockPassengers;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        mockTrip = new Trip();
        mockTrip.setId("trip-1");
        mockTrip.setPrice(new BigDecimal("500.00"));
        mockTrip.setDepartureCity("Mumbai");
        mockTrip.setDestinationCity("Delhi");
        mockTrip.setDepartureTime(LocalDateTime.now().plusDays(1));
        mockTrip.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(8));

        PassengerInfo passenger1 = new PassengerInfo();
        passenger1.setName("John Doe");
        passenger1.setPhone("9876543210");
        passenger1.setEmail("john@example.com");

        PassengerInfo passenger2 = new PassengerInfo();
        passenger2.setName("Jane Doe");
        passenger2.setPhone("9876543211");
        passenger2.setEmail("jane@example.com");

        mockPassengers = Arrays.asList(passenger1, passenger2);

        mockBookingRequest = new BookingRequest();
        mockBookingRequest.setTripId("trip-1");
        mockBookingRequest.setSeatNumbers(Arrays.asList("A1", "A2"));
        mockBookingRequest.setPassengers(mockPassengers);
        mockBookingRequest.setUserId("user-1");
        mockBookingRequest.setLockId("lock-123");
    }

    @Test
    void createBooking_ShouldSucceedWithValidRequest() {
        // Given
        when(valueOperations.get("lock:lock-123")).thenReturn("valid-lock");
        when(seatLockManager.isLocked("trip-1", "A1")).thenReturn(true);
        when(seatLockManager.isLocked("trip-1", "A2")).thenReturn(true);
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(mockTrip));
        when(bookingRepository.existsByPnr(anyString())).thenReturn(false);

        Booking savedBooking = new Booking();
        savedBooking.setId("booking-1");
        savedBooking.setPnr("ABC1234567");
        savedBooking.setTripId("trip-1");
        savedBooking.setUserId("user-1");
        savedBooking.setSeatNumbers("A1,A2");
        savedBooking.setTotalAmount(new BigDecimal("1230.00"));
        savedBooking.setTaxes(new BigDecimal("180.00"));
        savedBooking.setServiceFee(new BigDecimal("50.00"));
        savedBooking.setStatus(BookingStatus.PENDING);
        savedBooking.setCreatedAt(LocalDateTime.now());

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // When
        BookingResponse response = bookingService.createBooking(mockBookingRequest);

        // Then
        assertNotNull(response);
        assertEquals("booking-1", response.getId());
        assertEquals("ABC1234567", response.getPnr());
        assertEquals("trip-1", response.getTripId());
        assertEquals("user-1", response.getUserId());
        assertEquals(Arrays.asList("A1", "A2"), response.getSeatNumbers());
        assertEquals(new BigDecimal("1230.00"), response.getTotalAmount());
        assertEquals(new BigDecimal("180.00"), response.getTaxes());
        assertEquals(new BigDecimal("50.00"), response.getServiceFee());
        assertEquals("PENDING", response.getStatus());

        verify(bookingRepository).save(any(Booking.class));
        verify(valueOperations).set(eq("lock_booking:lock-123"), anyString(), eq(15L), any());
    }

    @Test
    void createBooking_ShouldFailWithExpiredLock() {
        // Given
        when(valueOperations.get("lock:lock-123")).thenReturn(null);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            bookingService.createBooking(mockBookingRequest);
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_ShouldFailWithMismatchedPassengerCount() {
        // Given
        when(valueOperations.get("lock:lock-123")).thenReturn("valid-lock");
        when(seatLockManager.isLocked("trip-1", "A1")).thenReturn(true);
        when(seatLockManager.isLocked("trip-1", "A2")).thenReturn(true);

        // Remove one passenger to create mismatch
        mockBookingRequest.setPassengers(Arrays.asList(mockPassengers.get(0)));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(mockBookingRequest);
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_ShouldFailWithInvalidTrip() {
        // Given
        when(valueOperations.get("lock:lock-123")).thenReturn("valid-lock");
        when(seatLockManager.isLocked("trip-1", "A1")).thenReturn(true);
        when(seatLockManager.isLocked("trip-1", "A2")).thenReturn(true);
        when(tripRepository.findById("trip-1")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(mockBookingRequest);
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void confirmBooking_ShouldSucceedForPendingBooking() {
        // Given
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setPnr("ABC1234567");
        booking.setTripId("trip-1");
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("1230.00"));

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(mockTrip));

        Booking confirmedBooking = new Booking();
        confirmedBooking.setId("booking-1");
        confirmedBooking.setPnr("ABC1234567");
        confirmedBooking.setTripId("trip-1");
        confirmedBooking.setStatus(BookingStatus.CONFIRMED);
        confirmedBooking.setPaymentId("payment-1");
        confirmedBooking.setConfirmedAt(LocalDateTime.now());
        confirmedBooking.setTotalAmount(new BigDecimal("1230.00"));

        when(bookingRepository.save(any(Booking.class))).thenReturn(confirmedBooking);

        // When
        BookingResponse response = bookingService.confirmBooking("booking-1", "payment-1");

        // Then
        assertNotNull(response);
        assertEquals("booking-1", response.getId());
        assertEquals("CONFIRMED", response.getStatus());
        assertNotNull(response.getConfirmedAt());

        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void confirmBooking_ShouldFailForNonPendingBooking() {
        // Given
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            bookingService.confirmBooking("booking-1", "payment-1");
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void cancelBooking_ShouldSucceedForValidBooking() {
        // Given
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setPnr("ABC1234567");
        booking.setUserId("user-1");
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // When
        assertDoesNotThrow(() -> {
            bookingService.cancelBooking("booking-1", "user-1");
        });

        // Then
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void cancelBooking_ShouldFailForUnauthorizedUser() {
        // Given
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setUserId("user-1");
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.cancelBooking("booking-1", "user-2");
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void getBooking_ShouldReturnBookingDetails() {
        // Given
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setPnr("ABC1234567");
        booking.setTripId("trip-1");
        booking.setUserId("user-1");
        booking.setSeatNumbers("A1,A2");
        booking.setTotalAmount(new BigDecimal("1230.00"));
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(mockTrip));

        // When
        BookingResponse response = bookingService.getBooking("booking-1");

        // Then
        assertNotNull(response);
        assertEquals("booking-1", response.getId());
        assertEquals("ABC1234567", response.getPnr());
        assertEquals("trip-1", response.getTripId());
        assertEquals("user-1", response.getUserId());
        assertEquals(Arrays.asList("A1", "A2"), response.getSeatNumbers());
        assertEquals("CONFIRMED", response.getStatus());
    }

    @Test
    void getBookingByPnr_ShouldReturnBookingDetails() {
        // Given
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setPnr("ABC1234567");
        booking.setTripId("trip-1");
        booking.setSeatNumbers("A1,A2");
        booking.setTotalAmount(new BigDecimal("1230.00"));
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByPnr("ABC1234567")).thenReturn(Optional.of(booking));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(mockTrip));

        // When
        BookingResponse response = bookingService.getBookingByPnr("ABC1234567");

        // Then
        assertNotNull(response);
        assertEquals("booking-1", response.getId());
        assertEquals("ABC1234567", response.getPnr());
        assertEquals("trip-1", response.getTripId());
        assertEquals("CONFIRMED", response.getStatus());
    }

    @Test
    void getUserBookings_ShouldReturnUserBookings() {
        // Given
        Booking booking1 = new Booking();
        booking1.setId("booking-1");
        booking1.setPnr("ABC1234567");
        booking1.setTripId("trip-1");
        booking1.setUserId("user-1");
        booking1.setSeatNumbers("A1");
        booking1.setTotalAmount(new BigDecimal("615.00"));
        booking1.setStatus(BookingStatus.CONFIRMED);

        Booking booking2 = new Booking();
        booking2.setId("booking-2");
        booking2.setPnr("XYZ9876543");
        booking2.setTripId("trip-1");
        booking2.setUserId("user-1");
        booking2.setSeatNumbers("A2");
        booking2.setTotalAmount(new BigDecimal("615.00"));
        booking2.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(Arrays.asList(booking1, booking2));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(mockTrip));

        // When
        List<BookingResponse> responses = bookingService.getUserBookings("user-1");

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("booking-1", responses.get(0).getId());
        assertEquals("booking-2", responses.get(1).getId());
    }
}