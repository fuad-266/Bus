package com.busticket.service;

import com.busticket.dto.TripResponse;
import com.busticket.dto.TripSearchRequest;
import com.busticket.model.*;
import com.busticket.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final CityRepository cityRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final ObjectMapper objectMapper;

    public SearchService(CityRepository cityRepository,
            BusRepository busRepository,
            RouteRepository routeRepository,
            TripRepository tripRepository,
            ObjectMapper objectMapper) {
        this.cityRepository = cityRepository;
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Search for trips based on departure city, destination city, date, and
     * filters.
     * 
     * @param request the search request containing search criteria
     * @return a list of trip responses matching the criteria
     */
    public List<TripResponse> searchTrips(TripSearchRequest request) {
        // Find cities by name
        Optional<City> departureCity = cityRepository.findByNameIgnoreCase(request.getDepartureCity());
        Optional<City> destinationCity = cityRepository.findByNameIgnoreCase(request.getDestinationCity());

        if (departureCity.isEmpty() || destinationCity.isEmpty()) {
            return new ArrayList<>();
        }

        // Find routes between cities
        List<Route> routes = routeRepository.findByDepartureCityIdAndDestinationCityId(
                departureCity.get().getId(),
                destinationCity.get().getId());

        if (routes.isEmpty()) {
            return new ArrayList<>();
        }

        // Search trips for all routes
        List<Trip> allTrips = new ArrayList<>();
        for (Route route : routes) {
            List<Trip> routeTrips = tripRepository.searchTrips(
                    departureCity.get().getId(),
                    destinationCity.get().getId(),
                    request.getDate(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    request.getDepartureTimeStart(),
                    request.getDepartureTimeEnd(),
                    request.getBusTypes(),
                    request.getMinAvailableSeats(),
                    request.getBusOperators());
            allTrips.addAll(routeTrips);
        }

        // Convert to TripResponse and apply sorting
        List<TripResponse> tripResponses = allTrips.stream()
                .map(this::convertToTripResponse)
                .collect(Collectors.toList());

        // Apply sorting
        applySorting(tripResponses, request.getSortBy());

        return tripResponses;
    }

    /**
     * Get city suggestions based on prefix for auto-complete functionality.
     * 
     * @param prefix the prefix to search for
     * @return a list of city names starting with the prefix
     */
    @Cacheable(value = "citySuggestions", key = "#prefix")
    public List<String> getCitySuggestions(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return cityRepository.findByNameStartsWithIgnoreCaseOrderByName(prefix.trim())
                .stream()
                .map(City::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get available seats count for a specific trip.
     * 
     * @param tripId the trip ID
     * @return the number of available seats
     */
    public Integer getAvailableSeats(String tripId) {
        Integer availableSeats = tripRepository.calculateAvailableSeats(tripId);
        return availableSeats != null ? availableSeats : 0;
    }

    /**
     * Get detailed bus information including rating.
     * 
     * @param busId the bus ID
     * @return the bus details with calculated rating
     */
    public Optional<Bus> getBusDetails(String busId) {
        return busRepository.findById(busId);
    }

    /**
     * Filter trips by bus operator name.
     * 
     * @param trips        the list of trips to filter
     * @param operatorName the bus operator name
     * @return filtered list of trips
     */
    public List<TripResponse> filterByBusOperator(List<TripResponse> trips, String operatorName) {
        if (operatorName == null || operatorName.trim().isEmpty()) {
            return trips;
        }

        return trips.stream()
                .filter(trip -> trip.getBusCompany().equalsIgnoreCase(operatorName.trim()))
                .collect(Collectors.toList());
    }

    /**
     * Get all available departure cities.
     * 
     * @return a list of cities that serve as departure points
     */
    @Cacheable(value = "departureCities")
    public List<String> getAvailableDepartureCities() {
        return cityRepository.findAvailableDepartureCities()
                .stream()
                .map(City::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all available destination cities.
     * 
     * @return a list of cities that serve as destinations
     */
    @Cacheable(value = "destinationCities")
    public List<String> getAvailableDestinationCities() {
        return cityRepository.findAvailableDestinationCities()
                .stream()
                .map(City::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all available bus operators.
     * 
     * @return a list of unique bus operator names
     */
    @Cacheable(value = "busOperators")
    public List<String> getAvailableBusOperators() {
        return busRepository.findDistinctCompanyNames();
    }

    /**
     * Get destination cities reachable from a departure city.
     * 
     * @param departureCity the departure city name
     * @return a list of reachable destination cities
     */
    @Cacheable(value = "reachableCities", key = "#departureCity")
    public List<String> getReachableDestinations(String departureCity) {
        Optional<City> city = cityRepository.findByNameIgnoreCase(departureCity);
        if (city.isEmpty()) {
            return new ArrayList<>();
        }

        return cityRepository.findCitiesReachableFromDeparture(city.get().getId())
                .stream()
                .map(City::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Convert Trip entity to TripResponse DTO.
     * 
     * @param trip the trip entity
     * @return the trip response DTO
     */
    private TripResponse convertToTripResponse(Trip trip) {
        // Get bus details
        Optional<Bus> bus = busRepository.findById(trip.getBusId());
        if (bus.isEmpty()) {
            throw new IllegalStateException("Bus not found for trip: " + trip.getId());
        }

        // Get route details
        Optional<Route> route = routeRepository.findById(trip.getRouteId());
        if (route.isEmpty()) {
            throw new IllegalStateException("Route not found for trip: " + trip.getId());
        }

        // Get city names
        Optional<City> departureCity = cityRepository.findById(route.get().getDepartureCityId());
        Optional<City> destinationCity = cityRepository.findById(route.get().getDestinationCityId());

        if (departureCity.isEmpty() || destinationCity.isEmpty()) {
            throw new IllegalStateException("Cities not found for route: " + route.get().getId());
        }

        // Calculate available seats
        Integer availableSeats = getAvailableSeats(trip.getId());

        // Parse amenities from JSON
        List<String> amenities = parseAmenities(bus.get().getAmenities());

        // Calculate duration
        Duration duration = Duration.between(trip.getDepartureTime(), trip.getArrivalTime());

        return new TripResponse(
                trip.getId(),
                trip.getBusId(),
                bus.get().getCompanyName(),
                bus.get().getBusNumber(),
                departureCity.get().getName(),
                destinationCity.get().getName(),
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                duration.toMinutes(),
                availableSeats,
                bus.get().getTotalSeats(),
                trip.getPrice(),
                bus.get().getBusType(),
                amenities,
                null // Rating will be calculated separately if needed
        );
    }

    /**
     * Parse amenities from JSON string.
     * 
     * @param amenitiesJson the JSON string containing amenities
     * @return a list of amenity strings
     */
    private List<String> parseAmenities(String amenitiesJson) {
        if (amenitiesJson == null || amenitiesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(amenitiesJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            // Log error and return empty list
            return new ArrayList<>();
        }
    }

    /**
     * Apply sorting to trip responses based on sort option.
     * 
     * @param trips  the list of trips to sort
     * @param sortBy the sort option
     */
    private void applySorting(List<TripResponse> trips, String sortBy) {
        if (sortBy == null) {
            return;
        }

        switch (sortBy.toUpperCase()) {
            case "CHEAPEST":
                trips.sort(Comparator.comparing(TripResponse::getPrice));
                break;
            case "FASTEST":
                trips.sort(Comparator.comparing(TripResponse::getDuration));
                break;
            case "EARLIEST_DEPARTURE":
                trips.sort(Comparator.comparing(TripResponse::getDepartureTime));
                break;
            default:
                // No sorting applied for unknown sort options
                break;
        }
    }
}