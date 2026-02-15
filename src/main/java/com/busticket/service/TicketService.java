package com.busticket.service;

import com.busticket.model.Booking;
import com.busticket.model.BookingStatus;
import com.busticket.model.Trip;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.BusRepository;
import com.busticket.repository.TripRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.io.image.ImageDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final int QR_CODE_SIZE = 200;

    private final BookingRepository bookingRepository;
    private final BusRepository busRepository;
    private final TripRepository tripRepository;

    @Autowired
    public TicketService(BookingRepository bookingRepository,
            BusRepository busRepository,
            TripRepository tripRepository) {
        this.bookingRepository = bookingRepository;
        this.busRepository = busRepository;
        this.tripRepository = tripRepository;
    }

    /**
     * Generates a ticket for a confirmed booking
     */
    public Ticket generateTicket(String bookingId) {
        logger.info("Generating ticket for booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot generate ticket for non-confirmed booking");
        }

        Trip trip = tripRepository.findById(booking.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + booking.getTripId()));

        // Generate QR code
        String qrCodeData = generateQRCodeData(booking, trip);
        String qrCodeBase64 = generateQRCode(qrCodeData);

        Ticket ticket = new Ticket(
                booking.getPnr(),
                booking,
                trip,
                qrCodeBase64,
                LocalDateTime.now());

        logger.info("Ticket generated successfully for PNR: {}", booking.getPnr());
        return ticket;
    }

    /**
     * Retrieves a ticket by PNR
     */
    public Ticket getTicket(String pnr) {
        logger.info("Retrieving ticket for PNR: {}", pnr);

        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with PNR: " + pnr));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Ticket not available for non-confirmed booking");
        }

        return generateTicket(booking.getId());
    }

    /**
     * Generates a PDF ticket
     */
    public byte[] generatePDF(Ticket ticket) {
        logger.info("Generating PDF for PNR: {}", ticket.getPnr());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            Paragraph title = new Paragraph("BUS TICKET")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // PNR
            Paragraph pnr = new Paragraph("PNR: " + ticket.getPnr())
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(pnr);

            // Trip details
            document.add(new Paragraph("\nTRIP DETAILS").setFontSize(14).setBold());
            document.add(new Paragraph("From: " + ticket.getTrip().getDepartureCity()));
            document.add(new Paragraph("To: " + ticket.getTrip().getDestinationCity()));
            document.add(
                    new Paragraph("Departure: " + ticket.getTrip().getDepartureTime().format(DATE_TIME_FORMATTER)));
            document.add(new Paragraph("Arrival: " + ticket.getTrip().getArrivalTime().format(DATE_TIME_FORMATTER)));

            // Seat details
            List<String> seatNumbers = List.of(ticket.getBooking().getSeatNumbers().split(","));
            document.add(new Paragraph("Seats: " + String.join(", ", seatNumbers)));

            // Passenger details
            document.add(new Paragraph("\nPASSENGER DETAILS").setFontSize(14).setBold());
            // Note: In a real implementation, you'd parse the JSON passenger data
            document.add(new Paragraph("Number of Passengers: " + seatNumbers.size()));

            // Fare details
            document.add(new Paragraph("\nFARE DETAILS").setFontSize(14).setBold());
            document.add(new Paragraph("Base Fare: ₹" + (ticket.getBooking().getTotalAmount()
                    .subtract(ticket.getBooking().getTaxes())
                    .subtract(ticket.getBooking().getServiceFee()))));
            document.add(new Paragraph("Taxes: ₹" + ticket.getBooking().getTaxes()));
            document.add(new Paragraph("Service Fee: ₹" + ticket.getBooking().getServiceFee()));
            document.add(new Paragraph("Total Amount: ₹" + ticket.getBooking().getTotalAmount()).setBold());

            // QR Code
            if (ticket.getQrCode() != null) {
                try {
                    byte[] qrCodeBytes = Base64.getDecoder().decode(ticket.getQrCode());
                    Image qrImage = new Image(ImageDataFactory.create(qrCodeBytes))
                            .setWidth(150)
                            .setHeight(150);
                    document.add(new Paragraph("\nSCAN QR CODE FOR VERIFICATION").setFontSize(12).setBold());
                    document.add(qrImage);
                } catch (Exception e) {
                    logger.warn("Failed to add QR code to PDF: {}", e.getMessage());
                }
            }

            // Footer
            document.add(new Paragraph("\nThank you for choosing our service!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));

            document.close();

            logger.info("PDF generated successfully for PNR: {}", ticket.getPnr());
            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Failed to generate PDF for PNR: {}", ticket.getPnr(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    // Private helper methods

    private String generateQRCodeData(Booking booking, Trip trip) {
        // Create JSON-like string for QR code data
        return String.format(
                "{\"pnr\":\"%s\",\"tripId\":\"%s\",\"seats\":\"%s\",\"passengers\":%d,\"amount\":\"%.2f\"}",
                booking.getPnr(),
                trip.getId(),
                booking.getSeatNumbers(),
                booking.getSeatNumbers().split(",").length,
                booking.getTotalAmount());
    }

    private String generateQRCode(String data) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (WriterException | IOException e) {
            logger.error("Failed to generate QR code: {}", e.getMessage(), e);
            throw new RuntimeException("QR code generation failed: " + e.getMessage(), e);
        }
    }

    // Inner class for Ticket
    public static class Ticket {
        private final String pnr;
        private final Booking booking;
        private final Trip trip;
        private final String qrCode;
        private final LocalDateTime generatedAt;

        public Ticket(String pnr, Booking booking, Trip trip, String qrCode, LocalDateTime generatedAt) {
            this.pnr = pnr;
            this.booking = booking;
            this.trip = trip;
            this.qrCode = qrCode;
            this.generatedAt = generatedAt;
        }

        public String getPnr() {
            return pnr;
        }

        public Booking getBooking() {
            return booking;
        }

        public Trip getTrip() {
            return trip;
        }

        public String getQrCode() {
            return qrCode;
        }

        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }
    }
}