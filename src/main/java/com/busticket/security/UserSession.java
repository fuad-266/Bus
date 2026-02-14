package com.busticket.security;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserSession implements Serializable {
    
    private String sessionId;
    private String userId;
    private String email;
    private String role;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public UserSession() {
    }

    public UserSession(String sessionId, String userId, String email, String role, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
