package com.busticket.service;

import com.busticket.model.Booking;
import com.busticket.model.BookingStatus;
import com.busticket.repository.BookingRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SeatLockManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BookingRepository bookingRepository;

    private static final String LOCK_PREFIX = "lock:";
    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    private static final long LOCK_TIMEOUT_MINUTES = 10;

    public SeatLockManager(RedisTemplate<String, Object> redisTemplate,
                          BookingRepository bookingRepository) {
        this.redisTemplate = redisTemplate;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Acquire locks for multiple seats on a trip.
     * 
     * @param tripId the trip ID
     * @param seatNumbers the list of seat numbers to lock
     * @param userId the user ID acquiring the locks
     * @return LockInfo if successful, null if any seat is already locked or booked
     */
    public LockInfo acquireLock(String tripId, List<String> seatNumbers, String userId) {
        // Check if any seat is already locked or booked
        for (String seatNumber : seatNumbers) {
            if (isLocked(tripId, seatNumber)) {
                return null; // Seat already locked
            }
            
            if (isBooked(tripId, seatNumber)) {
                return null; // Seat already booked
            }
        }

        // Create lock
        String lockId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(LOCK_TIMEOUT_MINUTES);

        LockInfo lockInfo = new LockInfo(lockId, tripId, seatNumbers, userId, expiresAt);

        // Store lock info in Redis with TTL
        String lockKey = LOCK_PREFIX + lockId;
        redisTemplate.opsForValue().set(lockKey, lockInfo, LOCK_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        // Store seat-to-lock mapping for each seat
        for (String seatNumber : seatNumbers) {
            String seatLockKey = SEAT_LOCK_PREFIX + tripId + ":" + seatNumber;
            redisTemplate.opsForValue().set(seatLockKey, lockId, LOCK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }

        return lockInfo;
    }

    /**
     * Release a lock and free all associated seats.
     * 
     * @param lockId the lock ID to release
     * @return true if lock was released, false if lock didn't exist
     */
    public boolean releaseLock(String lockId) {
        String lockKey = LOCK_PREFIX + lockId;
        LockInfo lockInfo = (LockInfo) redisTemplate.opsForValue().get(lockKey);

        if (lockInfo == null) {
            return false; // Lock already expired or doesn't exist
        }

        // Remove seat locks
        for (String seatNumber : lockInfo.getSeatNumbers()) {
            String seatLockKey = SEAT_LOCK_PREFIX + lockInfo.getTripId() + ":" + seatNumber;
            redisTemplate.delete(seatLockKey);
        }

        // Remove lock
        redisTemplate.delete(lockKey);
        return true;
    }

    /**
     * Extend the expiration time of a lock.
     * 
     * @param lockId the lock ID to extend
     * @param additionalMinutes additional minutes to extend the lock
     * @return true if lock was extended, false if lock doesn't exist
     */
    public boolean extendLock(String lockId, int additionalMinutes) {
        String lockKey = LOCK_PREFIX + lockId;
        LockInfo lockInfo = (LockInfo) redisTemplate.opsForValue().get(lockKey);

        if (lockInfo == null) {
            return false; // Lock doesn't exist
        }

        // Update expiration time
        LocalDateTime newExpiresAt = lockInfo.getExpiresAt().plusMinutes(additionalMinutes);
        lockInfo.setExpiresAt(newExpiresAt);

        // Calculate new TTL
        long newTtlMinutes = LOCK_TIMEOUT_MINUTES + additionalMinutes;

        // Update lock info with new TTL
        redisTemplate.opsForValue().set(lockKey, lockInfo, newTtlMinutes, TimeUnit.MINUTES);

        // Update seat locks with new TTL
        for (String seatNumber : lockInfo.getSeatNumbers()) {
            String seatLockKey = SEAT_LOCK_PREFIX + lockInfo.getTripId() + ":" + seatNumber;
            redisTemplate.opsForValue().set(seatLockKey, lockId, newTtlMinutes, TimeUnit.MINUTES);
        }

        return true;
    }

    /**
     * Check if a specific seat is locked.
     * 
     * @param tripId the trip ID
     * @param seatNumber the seat number
     * @return true if the seat is locked, false otherwise
     */
    public boolean isLocked(String tripId, String seatNumber) {
        String seatLockKey = SEAT_LOCK_PREFIX + tripId + ":" + seatNumber;
        String lockId = (String) redisTemplate.opsForValue().get(seatLockKey);
        return lockId != null;
    }

    /**
     * Check if a specific seat is booked (confirmed booking).
     * 
     * @param tripId the trip ID
     * @param seatNumber the seat number
     * @return true if the seat is booked, false otherwise
     */
    public boolean isBooked(String tripId, String seatNumber) {
        List<Booking> confirmedBookings = bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED);
        
        for (Booking booking : confirmedBookings) {
            try {
                // Parse seat numbers from JSON
                String seatNumbersJson = booking.getSeatNumbers();
                if (seatNumbersJson != null && seatNumbersJson.contains("\"" + seatNumber + "\"")) {
                    return true;
                }
            } catch (Exception e) {
                // Log error and continue checking other bookings
                continue;
            }
        }
        
        return false;
    }

    /**
     * Get lock information by lock ID.
     * 
     * @param lockId the lock ID
     * @return LockInfo if found, null otherwise
     */
    public LockInfo getLockInfo(String lockId) {
        String lockKey = LOCK_PREFIX + lockId;
        return (LockInfo) redisTemplate.opsForValue().get(lockKey);
    }

    /**
     * Check if a lock is still valid (not expired).
     * 
     * @param lockId the lock ID
     * @return true if lock is valid, false if expired or doesn't exist
     */
    public boolean isLockValid(String lockId) {
        LockInfo lockInfo = getLockInfo(lockId);
        if (lockInfo == null) {
            return false;
        }
        
        return lockInfo.getExpiresAt().isAfter(LocalDateTime.now());
    }

    /**
     * Get all locked seats for a trip.
     * 
     * @param tripId the trip ID
     * @return list of locked seat numbers
     */
    public List<String> getLockedSeats(String tripId) {
        // This would require scanning Redis keys, which is expensive
        // For now, we'll implement this by checking individual seats
        // In a production system, you might maintain a separate data structure
        throw new UnsupportedOperationException("getLockedSeats not implemented - use isLocked for individual seats");
    }

    /**
     * Clean up expired locks (called by scheduled job).
     * This method is mainly for cleanup of any orphaned data.
     */
    public void cleanupExpiredLocks() {
        // Redis TTL handles most cleanup automatically
        // This method can be used for additional cleanup if needed
        // For now, we rely on Redis TTL for automatic cleanup
    }

    /**
     * Information about a seat lock.
     */
    public static class LockInfo {
        private String lockId;
        private String tripId;
        private List<String> seatNumbers;
        private String userId;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;

        public LockInfo() {
        }

        public LockInfo(String lockId, String tripId, List<String> seatNumbers, String userId, LocalDateTime expiresAt) {
            this.lockId = lockId;
            this.tripId = tripId;
            this.seatNumbers = seatNumbers;
            this.userId = userId;
            this.expiresAt = expiresAt;
            this.createdAt = LocalDateTime.now();
        }

        // Getters and Setters
        public String getLockId() {
            return lockId;
        }

        public void setLockId(String lockId) {
            this.lockId = lockId;
        }

        public String getTripId() {
            return tripId;
        }

        public void setTripId(String tripId) {
            this.tripId = tripId;
        }

        public List<String> getSeatNumbers() {
            return seatNumbers;
        }

        public void setSeatNumbers(List<String> seatNumbers) {
            this.seatNumbers = seatNumbers;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}