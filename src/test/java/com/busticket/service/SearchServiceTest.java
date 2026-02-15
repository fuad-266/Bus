package com.busticket.service;

import com.busticket.dto.TripResponse;
import com.busticket.dto.TripSearchRequest;
import com.busticket.model.*;
import com.busticket.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SearchService searchService;

    private City mumbai;
    private City delhi;
    private Bus testBus;
    private Route testRoute;
    private Trip testTrip;

    @BeforeEach
    void setUp() {
        mumbai = new City();
        mumbai.setId("city-mumbai");
        mumbai.setName("Mumbai");

        delhi = new City();
        delhi.setId("city-delhi");
        delhi.setName("Delhi");

        testBus = new Bus();
        testBus.setId("bus-123");
        testBus.setCompanyName("Test Travels");
        testBus.setBusNumber("MH01AB1234");
        testBus.setBusType(BusType.AC);
        testBus.setTotalSeats(40);
        testBus.setAmenities("[\"WiFi\", \"Charging Port\"]");

        testRoute = new Route();
        testRoute.setId("route-123");
        testRoute.setDepartureCityId("city-mumbai");
        testRoute.setDestinationCityId("city-delhi");
        testRoute.setDistance(BigDecimal.valueOf(1400.0));
        testRoute.setEstimatedDuration(1200); // 20 hours

        testTrip = new Trip();
        testTrip.setId("trip-123");
        testTrip.setRouteId("route-123");
        testTrip.setBusId("bus-123");
        testTrip.setDepartureTime(LocalDateTime.now().plusDays(1));
        testTrip.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(20));
        testTrip.setPrice(BigDecimal.valueOf(1500.0));
        testTrip.setIsOpen(true);
    }

    @Test
    void searchTrips_WithValidCities_ShouldReturnTrips() throws Exception {
        // Arrange
        TripSearchRequest request = new TripSearchRequest("Mumbai", "Delhi", LocalDate.now().plusDays(1));

        when(cityRepository.findByNameIgnoreCase("Mumbai")).thenReturn(Optional.of(mumbai));
        when(cityRepository.findByNameIgnoreCase("Delhi")).thenReturn(Optional.of(delhi));
        when(routeRepository.findByDepartureCityIdAndDestinationCityId("city-mumbai", "city-delhi"))
                .thenReturn(Arrays.asList(testRoute));
        when(tripRepository.searchTrips(anyString(), anyString(), any(LocalDate.class),
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Arrays.asList(testTrip));
        when(busRepository.findById("bus-123")).thenReturn(Optional.of(testBus));
        when(routeRepository.findById("route-123")).thenReturn(Optional.of(testRoute));
        when(cityRepository.findById("city-mumbai")).thenReturn(Optional.of(mumbai));
        when(cityRepository.findById("city-delhi")).thenReturn(Optional.of(delhi));
        when(tripRepository.calculateAvailableSeats("trip-123")).thenReturn(35);
        when(objectMapper.readValue(eq("[\"WiFi\", \"Charging Port\"]"),
                any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Arrays.asList("WiFi", "Charging Port"));

        // Act
        List<TripResponse> result = searchService.searchTrips(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        TripResponse tripResponse = result.get(0);
        assertEquals("trip-123", tripResponse.getId());
        assertEquals("Test Travels", tripResponse.getBusCompany());
        assertEquals("Mumbai", tripResponse.getDepartureCity());
        assertEquals("Delhi", tripResponse.getDestinationCity());
        assertEquals(35, tripResponse.getAvailableSeats());
        assertEquals(40, tripResponse.getTotalSeats());
        assertEquals(BigDecimal.valueOf(1500.0), tripResponse.getPrice());
        assertEquals(BusType.AC, tripResponse.getBusType());
        assertEquals(Arrays.asList("WiFi", "Charging Port"), tripResponse.getAmenities());
    }

    @Test
    void searchTrips_WithInvalidDepartureCity_ShouldReturnEmptyList() {
        // Arrange
        TripSearchRequest request = new TripSearchRequest("InvalidCity", "Delhi", LocalDate.now().plusDays(1));

        when(cityRepository.findByNameIgnoreCase("InvalidCity")).thenReturn(Optional.empty());
        when(cityRepository.findByNameIgnoreCase("Delhi")).thenReturn(Optional.of(delhi));

        // Act
        List<TripResponse> result = searchService.searchTrips(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void searchTrips_WithInvalidDestinationCity_ShouldReturnEmptyList() {
        // Arrange
        TripSearchRequest request = new TripSearchRequest("Mumbai", "InvalidCity", LocalDate.now().plusDays(1));

        when(cityRepository.findByNameIgnoreCase("Mumbai")).thenReturn(Optional.of(mumbai));
        when(cityRepository.findByNameIgnoreCase("InvalidCity")).thenReturn(Optional.empty());

        // Act
        List<TripResponse> result = searchService.searchTrips(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void searchTrips_WithNoRoutes_ShouldReturnEmptyList() {
        // Arrange
        TripSearchRequest request = new TripSearchRequest("Mumbai", "Delhi", LocalDate.now().plusDays(1));

        when(cityRepository.findByNameIgnoreCase("Mumbai")).thenReturn(Optional.of(mumbai));
        when(cityRepository.findByNameIgnoreCase("Delhi")).thenReturn(Optional.of(delhi));
        when(routeRepository.findByDepartureCityIdAndDestinationCityId("city-mumbai", "city-delhi"))
                .thenReturn(Arrays.asList());

        // Act
        List<TripResponse> result = searchService.searchTrips(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCitySuggestions_WithValidPrefix_ShouldReturnCityNames() {
        // Arrange
        String prefix = "Mum";
        City mumbai = new City();
        mumbai.setName("Mumbai");
        City mumbaiBandra = new City();
        mumbaiBandra.setName("Mumbai Bandra");

        when(cityRepository.findByNameStartsWithIgnoreCaseOrderByName(prefix))
                .thenReturn(Arrays.asList(mumbai, mumbaiBandra));

        // Act
        List<String> result = searchService.getCitySuggestions(prefix);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Mumbai", result.get(0));
        assertEquals("Mumbai Bandra", result.get(1));
    }

    @Test
    void getCitySuggestions_WithEmptyPrefix_ShouldReturnEmptyList() {
        // Act
        List<String> result = searchService.getCitySuggestions("");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(cityRepository);
    }

    @Test
    void getCitySuggestions_WithNullPrefix_ShouldReturnEmptyList() {
        // Act
        List<String> result = searchService.getCitySuggestions(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(cityRepository);
    }

    @Test
    void getAvailableSeats_WithValidTripId_ShouldReturnSeatCount() {
        // Arrange
        String tripId = "trip-123";
        when(tripRepository.calculateAvailableSeats(tripId)).thenReturn(25);

        // Act
        Integer result = searchService.getAvailableSeats(tripId);

        // Assert
        assertEquals(25, result);
    }

    @Test
    void getAvailableSeats_WithNullResult_ShouldReturnZero() {
        // Arrange
        String tripId = "trip-123";
        when(tripRepository.calculateAvailableSeats(tripId)).thenReturn(null);

        // Act
        Integer result = searchService.getAvailableSeats(tripId);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void getBusDetails_WithValidBusId_ShouldReturnBus() {
        // Arrange
        String busId = "bus-123";
        when(busRepository.findById(busId)).thenReturn(Optional.of(testBus));

        // Act
        Optional<Bus> result = searchService.getBusDetails(busId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testBus, result.get());
    }

    @Test
    void getBusDetails_WithInvalidBusId_ShouldReturnEmpty() {
        // Arrange
        String busId = "invalid-bus";
        when(busRepository.findById(busId)).thenReturn(Optional.empty());

        // Act
        Optional<Bus> result = searchService.getBusDetails(busId);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void filterByBusOperator_WithValidOperator_ShouldFilterTrips() {
        // Arrange
        TripResponse trip1 = createTripResponse("trip-1", "Test Travels");
        TripResponse trip2 = createTripResponse("trip-2", "Another Travels");
        TripResponse trip3 = createTripResponse("trip-3", "Test Travels");

        List<TripResponse> trips = Arrays.asList(trip1, trip2, trip3);

        // Act
        List<TripResponse> result = searchService.filterByBusOperator(trips, "Test Travels");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(trip -> "Test Travels".equals(trip.getBusCompany())));
    }

    @Test
    void filterByBusOperator_WithNullOperator_ShouldReturnAllTrips() {
        // Arrange
        TripResponse trip1 = createTripResponse("trip-1", "Test Travels");
        TripResponse trip2 = createTripResponse("trip-2", "Another Travels");

        List<TripResponse> trips = Arrays.asList(trip1, trip2);

        // Act
        List<TripResponse> result = searchService.filterByBusOperator(trips, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(trips, result);
    }

    @Test
    void filterByBusOperator_WithEmptyOperator_ShouldReturnAllTrips() {
        // Arrange
        TripResponse trip1 = createTripResponse("trip-1", "Test Travels");
        TripResponse trip2 = createTripResponse("trip-2", "Another Travels");

        List<TripResponse> trips = Arrays.asList(trip1, trip2);

        // Act
        List<TripResponse> result = searchService.filterByBusOperator(trips, "");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(trips, result);
    }

    @Test
    void getAvailableDepartureCities_ShouldReturnSortedCityNames() {
        // Arrange
        City mumbai = new City();
        mumbai.setName("Mumbai");
        City delhi = new City();
        delhi.setName("Delhi");
        City bangalore = new City();
        bangalore.setName("Bangalore");

        when(cityRepository.findAvailableDepartureCities())
                .thenReturn(Arrays.asList(mumbai, delhi, bangalore));

        // Act
        List<String> result = searchService.getAvailableDepartureCities();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Bangalore", result.get(0)); // Should be sorted
        assertEquals("Delhi", result.get(1));
        assertEquals("Mumbai", result.get(2));
    }

    @Test
    void getAvailableDestinationCities_ShouldReturnSortedCityNames() {
        // Arrange
        City mumbai = new City();
        mumbai.setName("Mumbai");
        City delhi = new City();
        delhi.setName("Delhi");

        when(cityRepository.findAvailableDestinationCities())
                .thenReturn(Arrays.asList(mumbai, delhi));

        // Act
        List<String> result = searchService.getAvailableDestinationCities();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Delhi", result.get(0)); // Should be sorted
        assertEquals("Mumbai", result.get(1));
    }

    @Test
    void getAvailableBusOperators_ShouldReturnOperatorNames() {
        // Arrange
        when(busRepository.findDistinctCompanyNames())
                .thenReturn(Arrays.asList("Test Travels", "Another Travels"));

        // Act
        List<String> result = searchService.getAvailableBusOperators();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Travels", result.get(0));
        assertEquals("Another Travels", result.get(1));
    }

    @Test
    void getReachableDestinations_WithValidCity_ShouldReturnDestinations() {
        // Arrange
        String departureCity = "Mumbai";
        City delhi = new City();
        delhi.setName("Delhi");
        City bangalore = new City();
        bangalore.setName("Bangalore");

        when(cityRepository.findByNameIgnoreCase(departureCity)).thenReturn(Optional.of(mumbai));
        when(cityRepository.findCitiesReachableFromDeparture("city-mumbai"))
                .thenReturn(Arrays.asList(delhi, bangalore));

        // Act
        List<String> result = searchService.getReachableDestinations(departureCity);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Bangalore", result.get(0)); // Should be sorted
        assertEquals("Delhi", result.get(1));
    }

    @Test
    void getReachableDestinations_WithInvalidCity_ShouldReturnEmptyList() {
        // Arrange
        String departureCity = "InvalidCity";

        when(cityRepository.findByNameIgnoreCase(departureCity)).thenReturn(Optional.empty());

        // Act
        List<String> result = searchService.getReachableDestinations(departureCity);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private TripResponse createTripResponse(String id, String busCompany) {
        return new TripResponse(
                id,
                "bus-123",
                busCompany,
                "MH01AB1234",
                "Mumbai",
                "Delhi",
                testTrip.getDepartureTime(),
                testTrip.getArrivalTime(),
                1200L,
                35,
                40,
                BigDecimal.valueOf(1500.0),
                BusType.AC,
                Arrays.asList("WiFi", "Charging Port"),
                null);
    }
}