package com.busticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRegistrationRequest {
    
    @NotNull(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;
    
    @NotNull(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
    private String password;
    
    @NotNull(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;
    
    @NotNull(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,20}$", message = "Phone number must be 10-20 digits")
    private String phone;

    public UserRegistrationRequest() {
    }

    public UserRegistrationRequest(String email, String password, String name, String phone) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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
}
