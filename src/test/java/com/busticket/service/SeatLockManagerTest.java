package com.busticket.service;

import com.busticket.model.Booking;
import com.busticket.model.BookingStatus;
import com.busticket.repository.BookingRepository;
import com.busticket.service.SeatLockManager.LockInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SeatLockManager seatLockManager;

    private final String tripId = "trip-123";
    private final String userId = "user-123";
    private final List<String> seatNumbers = Arrays.asList("A1", "A2");

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void acquireLock_WithAvailableSeats_ShouldCreateLock() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn(null); // No existing locks
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(Collections.emptyList()); // No bookings

        // Act
        LockInfo result = seatLockManager.acquireLock(tripId, seatNumbers, userId);

        // Assert
        assertNotNull(result);
        assertEquals(tripId, result.getTripId());
        assertEquals(seatNumbers, result.getSeatNumbers());
        assertEquals(userId, result.getUserId());
        assertNotNull(result.getLockId());
        assertTrue(result.getExpiresAt().isAfter(LocalDateTime.now()));

        // Verify Redis operations
        verify(valueOperations).set(startsWith("lock:"), eq(result), eq(10L), eq(TimeUnit.MINUTES));
        verify(valueOperations, times(2)).set(startsWith("seat_lock:"), eq(result.getLockId()), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void acquireLock_WithLockedSeat_ShouldReturnNull() {
        // Arrange
        when(valueOperations.get("seat_lock:" + tripId + ":A1")).thenReturn("existing-lock-id");
        when(valueOperations.get("seat_lock:" + tripId + ":A2")).thenReturn(null);

        // Act
        LockInfo result = seatLockManager.acquireLock(tripId, seatNumbers, userId);

        // Assert
        assertNull(result);
        verify(valueOperations, never()).set(startsWith("lock:"), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void acquireLock_WithBookedSeat_ShouldReturnNull() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn(null); // No locks
        
        Booking existingBooking = new Booking();
        existingBooking.setSeatNumbers("[\"A1\", \"B1\"]");
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(Arrays.asList(existingBooking));

        // Act
        LockInfo result = seatLockManager.acquireLock(tripId, seatNumbers, userId);

        // Assert
        assertNull(result);
        verify(valueOperations, never()).set(startsWith("lock:"), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void releaseLock_WithValidLock_ShouldReleaseLock() {
        // Arrange
        String lockId = "lock-123";
        LockInfo lockInfo = new LockInfo(lockId, tripId, seatNumbers, userId, LocalDateTime.now().plusMinutes(10));
        
        when(valueOperations.get("lock:" + lockId)).thenReturn(lockInfo);

        // Act
        boolean result = seatLockManager.releaseLock(lockId);

        // Assert
        assertTrue(result);
        verify(redisTemplate).delete("lock:" + lockId);
        verify(redisTemplate).delete("seat_lock:" + tripId + ":A1");
        verify(redisTemplate).delete("seat_lock:" + tripId + ":A2");
    }

    @Test
    void releaseLock_WithInvalidLock_ShouldReturnFalse() {
        // Arrange
        String lockId = "invalid-lock";
        when(valueOperations.get("lock:" + lockId)).thenReturn(null);

        // Act
        boolean result = seatLockManager.releaseLock(lockId);

        // Assert
        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void extendLock_WithValidLock_ShouldExtendExpiration() {
        // Arrange
        String lockId = "lock-123";
        LocalDateTime originalExpiry = LocalDateTime.now().plusMinutes(5);
        LockInfo lockInfo = new LockInfo(lockId, tripId, seatNumbers, userId, originalExpiry);
        
        when(valueOperations.get("lock:" + lockId)).thenReturn(lockInfo);

        // Act
        boolean result = seatLockManager.extendLock(lockId, 5);

        // Assert
        assertTrue(result);
        assertTrue(lockInfo.getExpiresAt().isAfter(originalExpiry));
        verify(valueOperations).set(eq("lock:" + lockId), eq(lockInfo), eq(15L), eq(TimeUnit.MINUTES));
        verify(valueOperations, times(2)).set(startsWith("seat_lock:"), eq(lockId), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    void extendLock_WithInvalidLock_ShouldReturnFalse() {
        // Arrange
        String lockId = "invalid-lock";
        when(valueOperations.get("lock:" + lockId)).thenReturn(null);

        // Act
        boolean result = seatLockManager.extendLock(lockId, 5);

        // Assert
        assertFalse(result);
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void isLocked_WithLockedSeat_ShouldReturnTrue() {
        // Arrange
        String seatNumber = "A1";
        when(valueOperations.get("seat_lock:" + tripId + ":" + seatNumber)).thenReturn("lock-123");

        // Act
        boolean result = seatLockManager.isLocked(tripId, seatNumber);

        // Assert
        assertTrue(result);
    }

    @Test
    void isLocked_WithUnlockedSeat_ShouldReturnFalse() {
        // Arrange
        String seatNumber = "A1";
        when(valueOperations.get("seat_lock:" + tripId + ":" + seatNumber)).thenReturn(null);

        // Act
        boolean result = seatLockManager.isLocked(tripId, seatNumber);

        // Assert
        assertFalse(result);
    }

    @Test
    void isBooked_WithBookedSeat_ShouldReturnTrue() {
        // Arrange
        String seatNumber = "A1";
        Booking booking = new Booking();
        booking.setSeatNumbers("[\"A1\", \"A2\"]");
        
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(Arrays.asList(booking));

        // Act
        boolean result = seatLockManager.isBooked(tripId, seatNumber);

        // Assert
        assertTrue(result);
    }

    @Test
    void isBooked_WithUnbookedSeat_ShouldReturnFalse() {
        // Arrange
        String seatNumber = "A1";
        Booking booking = new Booking();
        booking.setSeatNumbers("[\"B1\", \"B2\"]");
        
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(Arrays.asList(booking));

        // Act
        boolean result = seatLockManager.isBooked(tripId, seatNumber);

        // Assert
        assertFalse(result);
    }

    @Test
    void isBooked_WithNoBookings_ShouldReturnFalse() {
        // Arrange
        String seatNumber = "A1";
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        // Act
        boolean result = seatLockManager.isBooked(tripId, seatNumber);

        // Assert
        assertFalse(result);
    }

    @Test
    void getLockInfo_WithValidLock_ShouldReturnLockInfo() {
        // Arrange
        String lockId = "lock-123";
        LockInfo expectedLockInfo = new LockInfo(lockId, tripId, seatNumbers, userId, LocalDateTime.now().plusMinutes(10));
        
        when(valueOperations.get("lock:" + lockId)).thenReturn(expectedLockInfo);

        // Act
        LockInfo result = seatLockManager.getLockInfo(lockId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedLockInfo, result);
    }

    @Test
    void getLockInfo_WithInvalidLock_ShouldReturnNull() {
        // Arrange
        String lockId = "invalid-lock";
        when(valueOperations.get("lock:" + lockId)).thenReturn(null);

        // Act
        LockInfo result = seatLockManager.getLockInfo(lockId);

        // Assert
        assertNull(result);
    }

    @Test
    void isLockValid_WithValidLock_ShouldReturnTrue() {
        // Arrange
        String lockId = "lock-123";
        LockInfo lockInfo = new LockInfo(lockId, tripId, seatNumbers, userId, LocalDateTime.now().plusMinutes(10));
        
        when(valueOperations.get("lock:" + lockId)).thenReturn(lockInfo);

        // Act
        boolean result = seatLockManager.isLockValid(lockId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isLockValid_WithExpiredLock_ShouldReturnFalse() {
        // Arrange
        String lockId = "lock-123";
        LockInfo lockInfo = new LockInfo(lockId, tripId, seatNumbers, userId, LocalDateTime.now().minusMinutes(1));
        
        when(valueOperations.get("lock:" + lockId)).thenReturn(lockInfo);

        // Act
        boolean result = seatLockManager.isLockValid(lockId);

        // Assert
        assertFalse(result);
    }

    @Test
    void isLockValid_WithNonExistentLock_ShouldReturnFalse() {
        // Arrange
        String lockId = "invalid-lock";
        when(valueOperations.get("lock:" + lockId)).thenReturn(null);

        // Act
        boolean result = seatLockManager.isLockValid(lockId);

        // Assert
        assertFalse(result);
    }

    @Test
    void lockInfo_ShouldHaveCorrectProperties() {
        // Arrange
        String lockId = "lock-123";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        // Act
        LockInfo lockInfo = new LockInfo(lockId, tripId, seatNumbers, userId, expiresAt);

        // Assert
        assertEquals(lockId, lockInfo.getLockId());
        assertEquals(tripId, lockInfo.getTripId());
        assertEquals(seatNumbers, lockInfo.getSeatNumbers());
        assertEquals(userId, lockInfo.getUserId());
        assertEquals(expiresAt, lockInfo.getExpiresAt());
        assertNotNull(lockInfo.getCreatedAt());
        assertTrue(lockInfo.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void getLockedSeats_ShouldThrowUnsupportedOperationException() {
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> {
            seatLockManager.getLockedSeats(tripId);
        });
    }

    @Test
    void cleanupExpiredLocks_ShouldNotThrowException() {
        // Act & Assert (should not throw any exception)
        assertDoesNotThrow(() -> {
            seatLockManager.cleanupExpiredLocks();
        });
    }
}