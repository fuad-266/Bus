package com.busticket.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaymentRequest {
    
    @NotNull(message = "Booking ID is required")
    private String bookingId;
    
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    @NotNull(message = "Payment method is required")
    private String method;
    
    private String cardNumber;
    private String expiryDate;
    private String cvv;

    public PaymentRequest() {
    }

    public PaymentRequest(String bookingId, BigDecimal amount, String method) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
    }

    // Getters and Setters
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
}
