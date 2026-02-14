package com.busticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PassengerInfo {
    
    @NotNull(message = "Passenger name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;
    
    @NotNull(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,20}$", message = "Phone number must be 10-20 digits")
    private String phone;
    
    @NotNull(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    public PassengerInfo() {
    }

    public PassengerInfo(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
