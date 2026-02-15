package com.busticket.service;

import com.busticket.model.Booking;
import com.busticket.model.BookingStatus;
import com.busticket.model.Trip;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.BusRepository;
import com.busticket.repository.TripRepository;
import com.busticket.service.TicketService.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TicketService ticketService;

    private Booking mockBooking;
    private Trip mockTrip;

    @BeforeEach
    void setUp() {
        mockTrip = new Trip();
        mockTrip.setId("trip-1");
        mockTrip.setDepartureCity("Mumbai");
        mockTrip.setDestinationCity("Delhi");
        mockTrip.setDepartureTime(LocalDateTime.now().plusDays(1));
        mockTrip.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(8));
        mockTrip.setPrice(new BigDecimal("500.00"));

        mockBooking = new Booking();
        mockBooking.setId("booking-1");
        mockBooking.setPnr("ABC1234567");
        mockBooking.setTripId("trip-1");
        mockBooking.setUserId("user-1");
        mockBooking.setSeatNumbers("A1,A2");
        mockBooking.setTotalAmount(new BigDecimal("1230.00"));
        mockBooking.setTaxes(new BigDecimal("180.00"));
        mockBooking.setServiceFee(new BigDecimal("50.00"));
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        mockBooking.setCreatedAt(LocalDateTime.now().minusHours(1));
        mockBooking.setConfirmedAt(LocalDateTime.now().minusMinutes(30));
    }

    @Test
    void generateTicket_ShouldSucceedForConfirmedBooking() {
        // Given
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(mockBooking));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(mockTrip));

        // When
        Ticket ticket = ticketService.generateTicket("booking-1");

        // Then
        assertNotNull(ticket);
        assertEquals("ABC1234567", ticket.getPnr());
        assertEquals(mockBooking, ticket.getBooking());
        assertEquals(mockTrip, ticket.getTrip());
        assertNotNull(ticket.getQrCode());
        assertNotNull(ticket.getGeneratedAt());
        assertTrue(ticket.getGeneratedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(ticket.getGeneratedAt().isAfter(LocalDateTime.now().minusSeconds(10)));
    }

    @Test
    void generateTicket_ShouldFailForNonExistentBooking() {
        // Given
        when(bookingRepository.findById("invalid-booking")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ticketService.generateTicket("invalid-booking");
        });

        verify(tripRepository, never()).findById(anyString());
    }

    @Test
    void generateTicket_ShouldFailForNonConfirmedBooking() {
        // Given
        mockBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(mockBooking));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            ticketService.generateTicket("booking-1");
        });

        verify(tripRepository, never()).findById(anyString());
    }

    @Test
    void generateTicket_ShouldFailForNonExistentTrip() {
        // Given
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(mockBooking));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ticketService.generateTicket("booking-1");
        });
    }

    @Test
    void getTicket_ShouldSucceedWithValidPNR() {
        // Given
        when(bookingRepository.findByPnr("ABC1234567")).thenReturn(Optional.of(mockBooking));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(mockTrip));

        // When
        Ticket ticket = ticketService.getTicket("ABC1234567");

        // Then
        assertNotNull(ticket);
        assertEquals("ABC1234567", ticket.getPnr());
        assertEquals(mockBooking, ticket.getBooking());
        assertEquals(mockTrip, ticket.getTrip());
        assertNotNull(ticket.getQrCode());
    }

    @Test
    void getTicket_ShouldFailWithInvalidPNR() {
        // Given
        when(bookingRepository.findByPnr("INVALID123")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ticketService.getTicket("INVALID123");
        });

        verify(tripRepository, never()).findById(anyString());
    }

    @Test
    void getTicket_ShouldFailForNonConfirmedBooking() {
        // Given
        mockBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findByPnr("ABC1234567")).thenReturn(Optional.of(mockBooking));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            ticketService.getTicket("ABC1234567");
        });

        verify(tripRepository, never()).findById(anyString());
    }

    @Test
    void generatePDF_ShouldSucceedWithValidTicket() {
        // Given
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(mockBooking));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(mockTrip));

        Ticket ticket = ticketService.generateTicket("booking-1");

        // When
        byte[] pdfBytes = ticketService.generatePDF(ticket);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // Check PDF header (PDF files start with %PDF)
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader);
    }

    @Test
    void generatePDF_ShouldHandleTicketWithoutQRCode() {
        // Given
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(mockBooking));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(mockTrip));

        // Create ticket manually without QR code
        Ticket ticket = new Ticket(
                mockBooking.getPnr(),
                mockBooking,
                mockTrip,
                null, // No QR code
                LocalDateTime.now());

        // When
        byte[] pdfBytes = ticketService.generatePDF(ticket);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // Check PDF header
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader);
    }

    @Test
    void ticket_ShouldHaveCorrectProperties() {
        // Given
        LocalDateTime generatedAt = LocalDateTime.now();

        // When
        Ticket ticket = new Ticket("ABC1234567", mockBooking, mockTrip, "qr-code-data", generatedAt);

        // Then
        assertEquals("ABC1234567", ticket.getPnr());
        assertEquals(mockBooking, ticket.getBooking());
        assertEquals(mockTrip, ticket.getTrip());
        assertEquals("qr-code-data", ticket.getQrCode());
        assertEquals(generatedAt, ticket.getGeneratedAt());
    }
}