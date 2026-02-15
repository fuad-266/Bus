package com.busticket.repository;

import com.busticket.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, String> {
    
    /**
     * Find routes between specific departure and destination cities.
     * 
     * @param departureCityId the departure city ID
     * @param destinationCityId the destination city ID
     * @return a list of routes between the cities
     */
    List<Route> findByDepartureCityIdAndDestinationCityId(String departureCityId, String destinationCityId);
    
    /**
     * Find all routes departing from a specific city.
     * 
     * @param departureCityId the departure city ID
     * @return a list of routes departing from the city
     */
    List<Route> findByDepartureCityIdOrderByDestinationCityId(String departureCityId);
    
    /**
     * Find all routes arriving at a specific city.
     * 
     * @param destinationCityId the destination city ID
     * @return a list of routes arriving at the city
     */
    List<Route> findByDestinationCityIdOrderByDepartureCityId(String destinationCityId);
    
    /**
     * Check if a route exists between two cities.
     * 
     * @param departureCityId the departure city ID
     * @param destinationCityId the destination city ID
     * @return true if a route exists, false otherwise
     */
    boolean existsByDepartureCityIdAndDestinationCityId(String departureCityId, String destinationCityId);
    
    /**
     * Find routes with distance within a range.
     * 
     * @param minDistance the minimum distance
     * @param maxDistance the maximum distance
     * @return a list of routes within the distance range
     */
    List<Route> findByDistanceBetweenOrderByDistance(Double minDistance, Double maxDistance);
    
    /**
     * Find routes with estimated duration within a range.
     * 
     * @param minDuration the minimum duration in minutes
     * @param maxDuration the maximum duration in minutes
     * @return a list of routes within the duration range
     */
    List<Route> findByEstimatedDurationBetweenOrderByEstimatedDuration(Integer minDuration, Integer maxDuration);
    
    /**
     * Find the shortest route between two cities by distance.
     * 
     * @param departureCityId the departure city ID
     * @param destinationCityId the destination city ID
     * @return an Optional containing the shortest route if found
     */
    Optional<Route> findFirstByDepartureCityIdAndDestinationCityIdOrderByDistanceAsc(String departureCityId, String destinationCityId);
    
    /**
     * Find the fastest route between two cities by duration.
     * 
     * @param departureCityId the departure city ID
     * @param destinationCityId the destination city ID
     * @return an Optional containing the fastest route if found
     */
    Optional<Route> findFirstByDepartureCityIdAndDestinationCityIdOrderByEstimatedDurationAsc(String departureCityId, String destinationCityId);
    
    /**
     * Find all routes that have active trips.
     * 
     * @return a list of routes with active trips
     */
    @Query("SELECT DISTINCT r FROM Route r WHERE r.id IN " +
           "(SELECT t.routeId FROM Trip t WHERE t.departureTime > CURRENT_TIMESTAMP AND t.isOpen = true)")
    List<Route> findRoutesWithActiveTrips();
    
    /**
     * Count routes by departure city.
     * 
     * @param departureCityId the departure city ID
     * @return the number of routes from the departure city
     */
    long countByDepartureCityId(String departureCityId);
    
    /**
     * Count routes by destination city.
     * 
     * @param destinationCityId the destination city ID
     * @return the number of routes to the destination city
     */
    long countByDestinationCityId(String destinationCityId);
}