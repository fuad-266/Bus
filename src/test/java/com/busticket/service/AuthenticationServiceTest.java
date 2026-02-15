package com.busticket.service;

import com.busticket.dto.AuthenticationResponse;
import com.busticket.dto.UserLoginRequest;
import com.busticket.dto.UserRegistrationRequest;
import com.busticket.dto.UserResponse;
import com.busticket.model.User;
import com.busticket.model.UserRole;
import com.busticket.repository.UserRepository;
import com.busticket.security.JwtUtil;
import com.busticket.security.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void register_WithValidDetails_ShouldCreateUser() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
                "test@example.com",
                "password123",
                "Test User",
                "1234567890"
        );

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");
        
        User savedUser = new User();
        savedUser.setId("user-123");
        savedUser.setEmail(request.getEmail());
        savedUser.setPasswordHash("hashedPassword");
        savedUser.setName(request.getName());
        savedUser.setPhone(request.getPhone());
        savedUser.setIsEmailVerified(false);
        savedUser.setRole(UserRole.USER);
        savedUser.setCreatedAt(LocalDateTime.now());
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = authenticationService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals(request.getEmail(), response.getEmail());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getPhone(), response.getPhone());
        assertEquals(false, response.getIsEmailVerified());
        assertEquals("USER", response.getRole());
        
        verify(userRepository).existsByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
                "existing@example.com",
                "password123",
                "Test User",
                "1234567890"
        );

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.register(request)
        );
        
        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthenticationResponse() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        UserLoginRequest request = new UserLoginRequest("test@example.com", "password123");
        
        User user = new User();
        user.setId("user-123");
        user.setEmail(request.getEmail());
        user.setPasswordHash("hashedPassword");
        user.setName("Test User");
        user.setRole(UserRole.USER);
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name()))
                .thenReturn("jwt-token");
        when(jwtUtil.getExpirationInSeconds()).thenReturn(86400L);

        // Act
        AuthenticationResponse response = authenticationService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(user.getId(), response.getUserId());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getName(), response.getName());
        assertEquals("USER", response.getRole());
        assertEquals(86400L, response.getExpiresIn());
        
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), user.getPasswordHash());
        verify(jwtUtil).generateToken(user.getId(), user.getEmail(), user.getRole().name());
        verify(valueOperations).set(
                startsWith("session:"),
                any(UserSession.class),
                eq(24L),
                eq(TimeUnit.HOURS)
        );
    }

    @Test
    void login_WithInvalidEmail_ShouldThrowException() {
        // Arrange
        UserLoginRequest request = new UserLoginRequest("nonexistent@example.com", "password123");
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.login(request)
        );
        
        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowException() {
        // Arrange
        UserLoginRequest request = new UserLoginRequest("test@example.com", "wrongpassword");
        
        User user = new User();
        user.setId("user-123");
        user.setEmail(request.getEmail());
        user.setPasswordHash("hashedPassword");
        user.setRole(UserRole.USER);
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.login(request)
        );
        
        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), user.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    void logout_ShouldDeleteSessionFromRedis() {
        // Arrange
        String sessionId = "session-123";
        String sessionKey = "session:" + sessionId;

        // Act
        authenticationService.logout(sessionId);

        // Assert
        verify(redisTemplate).delete(sessionKey);
    }

    @Test
    void getSession_WithValidSessionId_ShouldReturnSession() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        String sessionId = "session-123";
        String sessionKey = "session:" + sessionId;
        
        UserSession session = new UserSession(
                sessionId,
                "user-123",
                "test@example.com",
                "USER",
                LocalDateTime.now().plusHours(24)
        );
        
        when(valueOperations.get(sessionKey)).thenReturn(session);

        // Act
        UserSession result = authenticationService.getSession(sessionId);

        // Assert
        assertNotNull(result);
        assertEquals(sessionId, result.getSessionId());
        assertEquals("user-123", result.getUserId());
        assertEquals("test@example.com", result.getEmail());
        verify(valueOperations).get(sessionKey);
    }

    @Test
    void getSession_WithInvalidSessionId_ShouldReturnNull() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        String sessionId = "invalid-session";
        String sessionKey = "session:" + sessionId;
        
        when(valueOperations.get(sessionKey)).thenReturn(null);

        // Act
        UserSession result = authenticationService.getSession(sessionId);

        // Assert
        assertNull(result);
        verify(valueOperations).get(sessionKey);
    }

    @Test
    void isSessionValid_WithValidSession_ShouldReturnTrue() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        String sessionId = "session-123";
        String sessionKey = "session:" + sessionId;
        
        UserSession session = new UserSession(
                sessionId,
                "user-123",
                "test@example.com",
                "USER",
                LocalDateTime.now().plusHours(24)
        );
        
        when(valueOperations.get(sessionKey)).thenReturn(session);

        // Act
        boolean result = authenticationService.isSessionValid(sessionId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isSessionValid_WithExpiredSession_ShouldReturnFalse() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        String sessionId = "session-123";
        String sessionKey = "session:" + sessionId;
        
        UserSession session = new UserSession(
                sessionId,
                "user-123",
                "test@example.com",
                "USER",
                LocalDateTime.now().minusHours(1) // Expired 1 hour ago
        );
        
        when(valueOperations.get(sessionKey)).thenReturn(session);

        // Act
        boolean result = authenticationService.isSessionValid(sessionId);

        // Assert
        assertFalse(result);
    }

    @Test
    void isSessionValid_WithNonExistentSession_ShouldReturnFalse() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        String sessionId = "nonexistent-session";
        String sessionKey = "session:" + sessionId;
        
        when(valueOperations.get(sessionKey)).thenReturn(null);

        // Act
        boolean result = authenticationService.isSessionValid(sessionId);

        // Assert
        assertFalse(result);
    }
}
