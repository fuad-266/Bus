package com.busticket.repository;

import com.busticket.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, String> {
    
    /**
     * Find a city by its name (case-insensitive).
     * 
     * @param name the city name to search for
     * @return an Optional containing the city if found, or empty if not found
     */
    Optional<City> findByNameIgnoreCase(String name);
    
    /**
     * Find cities whose names start with the given prefix (case-insensitive).
     * Used for auto-suggestion functionality.
     * 
     * @param prefix the prefix to search for
     * @return a list of cities whose names start with the prefix
     */
    List<City> findByNameStartsWithIgnoreCaseOrderByName(String prefix);
    
    /**
     * Find all cities that are available as departure cities in routes.
     * 
     * @return a list of cities that serve as departure cities
     */
    @Query("SELECT DISTINCT c FROM City c WHERE c.id IN " +
           "(SELECT r.departureCityId FROM Route r)")
    List<City> findAvailableDepartureCities();
    
    /**
     * Find all cities that are available as destination cities in routes.
     * 
     * @return a list of cities that serve as destination cities
     */
    @Query("SELECT DISTINCT c FROM City c WHERE c.id IN " +
           "(SELECT r.destinationCityId FROM Route r)")
    List<City> findAvailableDestinationCities();
    
    /**
     * Find cities by state (case-insensitive).
     * 
     * @param state the state to search for
     * @return a list of cities in the specified state
     */
    List<City> findByStateIgnoreCaseOrderByName(String state);
    
    /**
     * Check if a city exists with the given name (case-insensitive).
     * 
     * @param name the city name to check
     * @return true if a city exists with this name, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * Find cities that have routes to a specific destination city.
     * 
     * @param destinationCityId the destination city ID
     * @return a list of cities that have routes to the destination
     */
    @Query("SELECT DISTINCT c FROM City c WHERE c.id IN " +
           "(SELECT r.departureCityId FROM Route r WHERE r.destinationCityId = :destinationCityId)")
    List<City> findCitiesWithRoutesToDestination(@Param("destinationCityId") String destinationCityId);
    
    /**
     * Find cities that can be reached from a specific departure city.
     * 
     * @param departureCityId the departure city ID
     * @return a list of cities that can be reached from the departure city
     */
    @Query("SELECT DISTINCT c FROM City c WHERE c.id IN " +
           "(SELECT r.destinationCityId FROM Route r WHERE r.departureCityId = :departureCityId)")
    List<City> findCitiesReachableFromDeparture(@Param("departureCityId") String departureCityId);
}