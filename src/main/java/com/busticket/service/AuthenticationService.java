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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_PREFIX = "session:";
    private static final long SESSION_EXPIRATION_HOURS = 24;

    public AuthenticationService(UserRepository userRepository, 
                                PasswordEncoder passwordEncoder,
                                JwtUtil jwtUtil,
                                RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Register a new user account.
     * 
     * @param request the registration request containing user details
     * @return UserResponse with the created user information
     * @throws IllegalArgumentException if email already exists
     */
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Hash password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create user
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordHash);
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setIsEmailVerified(false);
        user.setRole(UserRole.USER);

        user = userRepository.save(user);

        // Convert to response
        return convertToUserResponse(user);
    }

    /**
     * Authenticate a user and create a session.
     * 
     * @param request the login request containing credentials
     * @return AuthenticationResponse with JWT token and user information
     * @throws IllegalArgumentException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public AuthenticationResponse login(UserLoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getId(), 
                user.getEmail(), 
                user.getRole().name()
        );

        // Create session in Redis
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_EXPIRATION_HOURS);
        
        UserSession session = new UserSession(
                sessionId,
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                expiresAt
        );

        // Store session in Redis with TTL
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(
                sessionKey, 
                session, 
                SESSION_EXPIRATION_HOURS, 
                TimeUnit.HOURS
        );

        // Return authentication response
        return new AuthenticationResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                jwtUtil.getExpirationInSeconds()
        );
    }

    /**
     * Logout a user by invalidating their session.
     * 
     * @param sessionId the session ID to invalidate
     */
    public void logout(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.delete(sessionKey);
    }

    /**
     * Get session information from Redis.
     * 
     * @param sessionId the session ID to retrieve
     * @return UserSession if found, null otherwise
     */
    public UserSession getSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        Object sessionObj = redisTemplate.opsForValue().get(sessionKey);
        
        if (sessionObj instanceof UserSession) {
            return (UserSession) sessionObj;
        }
        
        return null;
    }

    /**
     * Validate if a session is still active.
     * 
     * @param sessionId the session ID to validate
     * @return true if session exists and is not expired, false otherwise
     */
    public boolean isSessionValid(String sessionId) {
        UserSession session = getSession(sessionId);
        if (session == null) {
            return false;
        }
        
        return session.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getIsEmailVerified(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
