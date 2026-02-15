package com.busticket.repository;

import com.busticket.model.BusType;
import com.busticket.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {
    
    /**
     * Find trips by route and date with basic filtering.
     * 
     * @param routeId the route ID
     * @param date the travel date
     * @return a list of trips for the route on the specified date
     */
    @Query("SELECT t FROM Trip t WHERE t.routeId = :routeId " +
           "AND DATE(t.departureTime) = :date " +
           "AND t.departureTime > CURRENT_TIMESTAMP " +
           "AND t.isOpen = true " +
           "ORDER BY t.departureTime")
    List<Trip> findAvailableTripsByRouteAndDate(@Param("routeId") String routeId, @Param("date") LocalDate date);
    
    /**
     * Search trips with comprehensive filtering and sorting.
     * 
     * @param departureCityId the departure city ID
     * @param destinationCityId the destination city ID
     * @param date the travel date
     * @param minPrice the minimum price (optional)
     * @param maxPrice the maximum price (optional)
     * @param departureTimeStart the earliest departure time (optional)
     * @param departureTimeEnd the latest departure time (optional)
     * @param busTypes the list of bus types to filter by (optional)
     * @param minAvailableSeats the minimum available seats (optional)
     * @param busOperators the list of bus operators to filter by (optional)
     * @return a list of trips matching the criteria
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN Route r ON t.routeId = r.id " +
           "JOIN Bus b ON t.busId = b.id " +
           "WHERE r.departureCityId = :departureCityId " +
           "AND r.destinationCityId = :destinationCityId " +
           "AND DATE(t.departureTime) = :date " +
           "AND t.departureTime > CURRENT_TIMESTAMP " +
           "AND t.isOpen = true " +
           "AND (:minPrice IS NULL OR t.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR t.price <= :maxPrice) " +
           "AND (:departureTimeStart IS NULL OR TIME(t.departureTime) >= :departureTimeStart) " +
           "AND (:departureTimeEnd IS NULL OR TIME(t.departureTime) <= :departureTimeEnd) " +
           "AND (:busTypes IS NULL OR b.busType IN :busTypes) " +
           "AND (:minAvailableSeats IS NULL OR (b.totalSeats - " +
           "    (SELECT COUNT(bk.id) FROM Booking bk WHERE bk.tripId = t.id AND bk.status = 'CONFIRMED')) >= :minAvailableSeats) " +
           "AND (:busOperators IS NULL OR b.companyName IN :busOperators)")
    List<Trip> searchTrips(
            @Param("departureCityId") String departureCityId,
            @Param("destinationCityId") String destinationCityId,
            @Param("date") LocalDate date,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("departureTimeStart") LocalTime departureTimeStart,
            @Param("departureTimeEnd") LocalTime departureTimeEnd,
            @Param("busTypes") List<BusType> busTypes,
            @Param("minAvailableSeats") Integer minAvailableSeats,
            @Param("busOperators") List<String> busOperators
    );
    
    /**
     * Find trips by bus ID.
     * 
     * @param busId the bus ID
     * @return a list of trips for the bus
     */
    List<Trip> findByBusIdOrderByDepartureTimeDesc(String busId);
    
    /**
     * Find trips by route ID.
     * 
     * @param routeId the route ID
     * @return a list of trips for the route
     */
    List<Trip> findByRouteIdOrderByDepartureTimeDesc(String routeId);
    
    /**
     * Find trips within a price range.
     * 
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @return a list of trips within the price range
     */
    List<Trip> findByPriceBetweenOrderByPriceAsc(BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * Find trips departing within a time range on a specific date.
     * 
     * @param date the travel date
     * @param startTime the earliest departure time
     * @param endTime the latest departure time
     * @return a list of trips departing within the time range
     */
    @Query("SELECT t FROM Trip t WHERE DATE(t.departureTime) = :date " +
           "AND TIME(t.departureTime) BETWEEN :startTime AND :endTime " +
           "AND t.departureTime > CURRENT_TIMESTAMP " +
           "AND t.isOpen = true " +
           "ORDER BY t.departureTime")
    List<Trip> findTripsByDateAndTimeRange(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
    
    /**
     * Find upcoming trips (future departures).
     * 
     * @return a list of upcoming trips
     */
    List<Trip> findByDepartureTimeAfterAndIsOpenTrueOrderByDepartureTimeAsc(LocalDateTime currentTime);
    
    /**
     * Find past trips (completed departures).
     * 
     * @return a list of past trips
     */
    List<Trip> findByDepartureTimeBeforeOrderByDepartureTimeDesc(LocalDateTime currentTime);
    
    /**
     * Find trips managed by a specific bus admin.
     * 
     * @param adminUserId the admin user ID
     * @return a list of trips managed by the admin
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN Bus b ON t.busId = b.id " +
           "WHERE b.adminUserId = :adminUserId " +
           "ORDER BY t.departureTime DESC")
    List<Trip> findTripsByBusAdmin(@Param("adminUserId") String adminUserId);
    
    /**
     * Calculate available seats for a trip.
     * 
     * @param tripId the trip ID
     * @return the number of available seats
     */
    @Query("SELECT (b.totalSeats - COALESCE(SUM(CASE WHEN bk.status = 'CONFIRMED' THEN " +
           "    (SELECT COUNT(*) FROM json_array_elements_text(CAST(bk.seatNumbers AS json))) ELSE 0 END), 0)) " +
           "FROM Trip t " +
           "JOIN Bus b ON t.busId = b.id " +
           "LEFT JOIN Booking bk ON bk.tripId = t.id " +
           "WHERE t.id = :tripId " +
           "GROUP BY b.totalSeats")
    Integer calculateAvailableSeats(@Param("tripId") String tripId);
    
    /**
     * Find trips with minimum available seats.
     * 
     * @param minSeats the minimum number of available seats
     * @return a list of trips with at least the specified available seats
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN Bus b ON t.busId = b.id " +
           "WHERE t.departureTime > CURRENT_TIMESTAMP " +
           "AND t.isOpen = true " +
           "AND (b.totalSeats - COALESCE(" +
           "    (SELECT COUNT(*) FROM Booking bk WHERE bk.tripId = t.id AND bk.status = 'CONFIRMED'), 0)) >= :minSeats " +
           "ORDER BY t.departureTime")
    List<Trip> findTripsWithMinimumAvailableSeats(@Param("minSeats") Integer minSeats);
    
    /**
     * Count trips by bus admin.
     * 
     * @param adminUserId the admin user ID
     * @return the number of trips managed by the admin
     */
    @Query("SELECT COUNT(t) FROM Trip t " +
           "JOIN Bus b ON t.busId = b.id " +
           "WHERE b.adminUserId = :adminUserId")
    long countTripsByBusAdmin(@Param("adminUserId") String adminUserId);
    
    /**
     * Find cheapest trips for a route and date.
     * 
     * @param routeId the route ID
     * @param date the travel date
     * @return a list of trips ordered by price (cheapest first)
     */
    @Query("SELECT t FROM Trip t WHERE t.routeId = :routeId " +
           "AND DATE(t.departureTime) = :date " +
           "AND t.departureTime > CURRENT_TIMESTAMP " +
           "AND t.isOpen = true " +
           "ORDER BY t.price ASC")
    List<Trip> findCheapestTrips(@Param("routeId") String routeId, @Param("date") LocalDate date);
    
    /**
     * Find fastest trips for a route and date (by arrival time).
     * 
     * @param routeId the route ID
     * @param date the travel date
     * @return a list of trips ordered by duration (fastest first)
     */
    @Query("SELECT t FROM Trip t WHERE t.routeId = :routeId " +
           "AND DATE(t.departureTime) = :date " +
           "AND t.departureTime > CURRENT_TIMESTAMP " +
           "AND t.isOpen = true " +
           "ORDER BY (t.arrivalTime - t.departureTime) ASC")
    List<Trip> findFastestTrips(@Param("routeId") String routeId, @Param("date") LocalDate date);
    
    /**
     * Find earliest departure trips for a route and date.
     * 
     * @param routeId the route ID
     * @param date the travel date
     * @return a list of trips ordered by departure time (earliest first)
     */
    @Query("SELECT t FROM Trip t WHERE t.routeId = :routeId " +
           "AND DATE(t.departureTime) = :date " +
           "AND t.departureTime > CURRENT_TIMESTAMP " +
           "AND t.isOpen = true " +
           "ORDER BY t.departureTime ASC")
    List<Trip> findEarliestTrips(@Param("routeId") String routeId, @Param("date") LocalDate date);
}