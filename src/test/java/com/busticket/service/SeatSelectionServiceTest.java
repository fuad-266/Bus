package com.busticket.service;

import com.busticket.model.Bus;
import com.busticket.model.BusType;
import com.busticket.model.Trip;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.BusRepository;
import com.busticket.service.SeatSelectionService.FareSummary;
import com.busticket.service.SeatSelectionService.Seat;
import com.busticket.service.SeatSelectionService.SeatLayout;
import com.busticket.service.SeatSelectionService.SeatSelection;
import com.busticket.service.SeatSelectionService.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatSelectionServiceTest {

    @Mock
    private BusRepository busRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatLockManager seatLockManager;

    @InjectMocks
    private SeatSelectionService seatSelectionService;

    private Trip mockTrip;
    private Bus mockBus;

    @BeforeEach
    void setUp() {
        mockBus = new Bus();
        mockBus.setId("bus-1");
        mockBus.setBusType(BusType.AC_SLEEPER);
        mockBus.setSeatLayout("{\"rows\": 2, \"columns\": 2, \"seats\": [{\"number\": \"A1\", \"row\": 1, \"column\": 1}, {\"number\": \"A2\", \"row\": 1, \"column\": 2}, {\"number\": \"B1\", \"row\": 2, \"column\": 1}, {\"number\": \"B2\", \"row\": 2, \"column\": 2}]}");

        mockTrip = new Trip();
        mockTrip.setId("trip-1");
        mockTrip.setBus(mockBus);
        mockTrip.setPrice(new BigDecimal("500.00"));
    }

    @Test
    void getSeatLayout_ShouldReturnCorrectLayout() {
        // Given
        when(busRepository.findTripById("trip-1")).thenReturn(Optional.of(mockTrip));
        when(bookingRepository.findBookedSeatsByTripId("trip-1")).thenReturn(Arrays.asList("A1"));
        when(seatLockManager.isLocked("trip-1", "A2")).thenReturn(true);

        // When
        SeatLayout layout = seatSelectionService.getSeatLayout("trip-1");

        // Then
        assertNotNull(layout);
        assertEquals("trip-1", layout.getTripId());
        assertEquals(2, layout.getRows());
        assertEquals(2, layout.getColumns());
        assertEquals(4, layout.getSeats().size());

        // Check seat statuses
        Seat seatA1 = layout.getSeats().stream()
                .filter(s -> "A1".equals(s.getNumber()))
                .findFirst().orElse(null);
        assertNotNull(seatA1);
        assertEquals(SeatStatus.BOOKED, seatA1.getStatus());

        Seat seatA2 = layout.getSeats().stream()
                .filter(s -> "A2".equals(s.getNumber()))
                .findFirst().orElse(null);
        assertNotNull(seatA2);
        assertEquals(SeatStatus.LOCKED, seatA2.getStatus());

        Seat seatB1 = layout.getSeats().stream()
                .filter(s -> "B1".equals(s.getNumber()))
                .findFirst().orElse(null);
        assertNotNull(seatB1);
        assertEquals(SeatStatus.AVAILABLE, seatB1.getStatus());
    }

    @Test
    void selectSeat_ShouldSucceedForAvailableSeat() {
        // Given
        when(busRepository.findTripById("trip-1")).thenReturn(Optional.of(mockTrip));
        when(bookingRepository.findBookedSeatsByTripId("trip-1")).thenReturn(Arrays.asList());
        when(seatLockManager.isLocked("trip-1", "A1")).thenReturn(false);
        when(seatLockManager.acquireLock("trip-1", "A1", "user-1")).thenReturn("lock-123");

        // When
        SeatSelection result = seatSelectionService.selectSeat("trip-1", "A1", "user-1");

        // Then
        assertNotNull(result);
        assertEquals("A1", result.getSeatNumber());
        assertEquals("lock-123", result.getLockId());
        assertNotNull(result.getExpiresAt());
        verify(seatLockManager).acquireLock("trip-1", "A1", "user-1");
    }

    @Test
    void selectSeat_ShouldFailForBookedSeat() {
        // Given
        when(busRepository.findTripById("trip-1")).thenReturn(Optional.of(mockTrip));
        when(bookingRepository.findBookedSeatsByTripId("trip-1")).thenReturn(Arrays.asList("A1"));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            seatSelectionService.selectSeat("trip-1", "A1", "user-1");
        });
        verify(seatLockManager, never()).acquireLock(anyString(), anyString(), anyString());
    }

    @Test
    void selectSeat_ShouldFailForLockedSeat() {
        // Given
        when(busRepository.findTripById("trip-1")).thenReturn(Optional.of(mockTrip));
        when(bookingRepository.findBookedSeatsByTripId("trip-1")).thenReturn(Arrays.asList());
        when(seatLockManager.isLocked("trip-1", "A1")).thenReturn(true);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            seatSelectionService.selectSeat("trip-1", "A1", "user-1");
        });
        verify(seatLockManager, never()).acquireLock(anyString(), anyString(), anyString());
    }

    @Test
    void deselectSeat_ShouldReleaseLock() {
        // Given
        when(seatLockManager.releaseLock("trip-1", "A1", "user-1")).thenReturn(true);

        // When
        boolean result = seatSelectionService.deselectSeat("trip-1", "A1", "user-1");

        // Then
        assertTrue(result);
        verify(seatLockManager).releaseLock("trip-1", "A1", "user-1");
    }

    @Test
    void selectMultipleSeats_ShouldSucceedForAvailableSeats() {
        // Given
        List<String> seatNumbers = Arrays.asList("A1", "A2");
        when(busRepository.findTripById("trip-1")).thenReturn(Optional.of(mockTrip));
        when(bookingRepository.findBookedSeatsByTripId("trip-1")).thenReturn(Arrays.asList());
        when(seatLockManager.isLocked("trip-1", "A1")).thenReturn(false);
        when(seatLockManager.isLocked("trip-1", "A2")).thenReturn(false);
        when(seatLockManager.acquireLock("trip-1", "A1", "user-1")).thenReturn("lock-123");
        when(seatLockManager.acquireLock("trip-1", "A2", "user-1")).thenReturn("lock-124");

        // When
        List<SeatSelection> results = seatSelectionService.selectMultipleSeats("trip-1", seatNumbers, "user-1");

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("A1", results.get(0).getSeatNumber());
        assertEquals("A2", results.get(1).getSeatNumber());
        verify(seatLockManager).acquireLock("trip-1", "A1", "user-1");
        verify(seatLockManager).acquireLock("trip-1", "A2", "user-1");
    }

    @Test
    void calculateTotalPrice_ShouldReturnCorrectAmount() {
        // Given
        List<String> seatNumbers = Arrays.asList("A1", "A2");
        when(busRepository.findTripById("trip-1")).thenReturn(Optional.of(mockTrip));

        // When
        BigDecimal totalPrice = seatSelectionService.calculateTotalPrice("trip-1", seatNumbers);

        // Then
        assertEquals(new BigDecimal("1000.00"), totalPrice); // 2 seats * 500.00 each
    }

    @Test
    void getFareSummary_ShouldReturnCorrectBreakdown() {
        // Given
        List<String> seatNumbers = Arrays.asList("A1", "A2");
        when(busRepository.findTripById("trip-1")).thenReturn(Optional.of(mockTrip));

        // When
        FareSummary summary = seatSelectionService.getFareSummary("trip-1", seatNumbers);

        // Then
        assertNotNull(summary);
        assertEquals(new BigDecimal("1000.00"), summary.getBaseFare()); // 2 * 500.00
        assertEquals(new BigDecimal("180.00"), summary.getTaxes()); // 18% of base fare
        assertEquals(new BigDecimal("50.00"), summary.getServiceFee()); // Fixed service fee
        assertEquals(new BigDecimal("1230.00"), summary.getTotalAmount()); // Base + taxes + service fee
        assertEquals(2, summary.getSeatCount());
    }

    @Test
    void getSeatLayout_ShouldThrowExceptionForInvalidTrip() {
        // Given
        when(busRepository.findTripById("invalid-trip")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            seatSelectionService.getSeatLayout("invalid-trip");
        });
    }

    @Test
    void selectSeat_ShouldThrowExceptionForInvalidSeat() {
        // Given
        when(busRepository.findTripById("trip-1")).thenReturn(Optional.of(mockTrip));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            seatSelectionService.selectSeat("trip-1", "INVALID", "user-1");
        });
    }
}