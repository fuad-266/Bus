package com.busticket.repository;

import com.busticket.model.Bus;
import com.busticket.model.BusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, String> {
    
    /**
     * Find a bus by its bus number.
     * 
     * @param busNumber the bus number to search for
     * @return an Optional containing the bus if found, or empty if not found
     */
    Optional<Bus> findByBusNumber(String busNumber);
    
    /**
     * Find buses by company name (case-insensitive).
     * 
     * @param companyName the company name to search for
     * @return a list of buses operated by the company
     */
    List<Bus> findByCompanyNameIgnoreCaseOrderByBusNumber(String companyName);
    
    /**
     * Find buses by bus type.
     * 
     * @param busType the bus type to search for
     * @return a list of buses of the specified type
     */
    List<Bus> findByBusTypeOrderByCompanyName(BusType busType);
    
    /**
     * Find buses managed by a specific admin user.
     * 
     * @param adminUserId the admin user ID
     * @return a list of buses managed by the admin
     */
    List<Bus> findByAdminUserIdOrderByCompanyNameAscBusNumberAsc(String adminUserId);
    
    /**
     * Find buses by company name and bus type.
     * 
     * @param companyName the company name
     * @param busType the bus type
     * @return a list of buses matching both criteria
     */
    List<Bus> findByCompanyNameIgnoreCaseAndBusTypeOrderByBusNumber(String companyName, BusType busType);
    
    /**
     * Check if a bus exists with the given bus number.
     * 
     * @param busNumber the bus number to check
     * @return true if a bus exists with this number, false otherwise
     */
    boolean existsByBusNumber(String busNumber);
    
    /**
     * Find all distinct company names.
     * 
     * @return a list of unique company names
     */
    @Query("SELECT DISTINCT b.companyName FROM Bus b ORDER BY b.companyName")
    List<String> findDistinctCompanyNames();
    
    /**
     * Find buses with minimum seat capacity.
     * 
     * @param minSeats the minimum number of seats
     * @return a list of buses with at least the specified number of seats
     */
    List<Bus> findByTotalSeatsGreaterThanEqualOrderByTotalSeatsAsc(Integer minSeats);
    
    /**
     * Find buses that have specific amenities (JSON contains search).
     * 
     * @param amenity the amenity to search for
     * @return a list of buses that have the specified amenity
     */
    @Query("SELECT b FROM Bus b WHERE b.amenities LIKE %:amenity%")
    List<Bus> findBusesWithAmenity(@Param("amenity") String amenity);
    
    /**
     * Count buses by admin user.
     * 
     * @param adminUserId the admin user ID
     * @return the number of buses managed by the admin
     */
    long countByAdminUserId(String adminUserId);
    
    /**
     * Find buses that are currently assigned to active trips.
     * 
     * @return a list of buses that have active trips
     */
    @Query("SELECT DISTINCT b FROM Bus b WHERE b.id IN " +
           "(SELECT t.busId FROM Trip t WHERE t.departureTime > CURRENT_TIMESTAMP AND t.isOpen = true)")
    List<Bus> findBusesWithActiveTrips();
}