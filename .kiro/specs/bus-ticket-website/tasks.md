# Implementation Plan: Bus Ticket Website

## Overview

This implementation plan breaks down the bus ticket booking website into incremental, testable steps using Java. The system will be built as a Spring Boot application with a RESTful API backend, PostgreSQL database, Redis for caching, and a React frontend.

## Technology Stack

- **Backend**: Java 17+ with Spring Boot 3.x
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7+
- **Testing**: JUnit 5, Mockito, jqwik (property-based testing)
- **Frontend**: React 18+ with TypeScript
- **Build Tool**: Maven or Gradle

## Tasks

- [x] 1. Project setup and infrastructure
  - Create Spring Boot project with required dependencies (Spring Web, Spring Data JPA, Spring Security, Redis, PostgreSQL driver)
  - Configure database connection and Redis connection
  - Set up project structure (controllers, services, repositories, models, DTOs)
  - Configure application.properties for dev/staging/prod environments
  - Set up logging configuration
  - _Requirements: Infrastructure setup_

- [x] 2. Database schema and migrations
  - Create Flyway or Liquibase migration scripts for all tables (cities, buses, routes, trips, users, bookings, payments, refunds, trip_ratings)
  - Add database indexes as specified in design
  - Create database constraints and foreign keys
  - Test migrations on local PostgreSQL instance
  - _Requirements: All requirements (data foundation)_

- [x] 3. Implement core domain models and entities
  - Create JPA entities for City, Bus, Route, Trip, User, Booking, Payment, Refund, TripRating
  - Add proper annotations (@Entity, @Table, @Column, @Id, @GeneratedValue)
  - Define enums (BusType, UserRole, BookingStatus, PaymentStatus, SeatStatus)
  - Add validation annotations (@NotNull, @Size, @Email, @Pattern)
  - Create DTOs for API requests and responses
  - _Requirements: All requirements (data models)_

- [x] 4. Implement authentication and authorization
  - [x] 4.1 Create User entity and repository
    - Implement UserRepository with Spring Data JPA
    - Add methods for findByEmail, existsByEmail
    - _Requirements: 5.1, 5.2_
  
  - [x] 4.2 Implement authentication service
    - Create AuthenticationService with register, login, logout methods
    - Implement password hashing with BCrypt
    - Implement JWT token generation and validation
    - Create session management with Redis
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  
  - [ ]* 4.3 Write property tests for authentication
    - **Property 27: Valid registration creates account**
    - **Property 28: Valid login creates session**
    - **Property 30: Logout invalidates session**
    - **Validates: Requirements 5.1, 5.2, 5.4**
  
  - [ ]* 4.4 Write unit tests for authentication edge cases
    - Test duplicate email registration
    - Test invalid credentials
    - Test password reset flow
    - _Requirements: 5.1, 5.2, 5.3_

- [x] 5. Implement search and discovery service
  - [x] 5.1 Create City, Bus, Route, Trip repositories
    - Implement repositories with Spring Data JPA
    - Add custom query methods for search
    - _Requirements: 1.1, 1.2, 1.3_
  
  - [x] 5.2 Implement SearchService
    - Create searchTrips method with filtering and sorting
    - Implement getCitySuggestions with prefix matching
    - Implement getBusDetails method
    - Implement filterByBusOperator method
    - Add caching for city suggestions (Redis)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11_
  
  - [ ]* 5.3 Write property tests for search functionality
    - **Property 1: City auto-suggestion correctness**
    - **Property 2: Search results match criteria**
    - **Property 4: Price filter correctness**
    - **Property 5: Time filter correctness**
    - **Property 6: Bus type filter correctness**
    - **Property 7: Available seats filter correctness**
    - **Property 8: Sort by price correctness**
    - **Property 9: Sort by duration correctness**
    - **Property 10: Sort by departure time correctness**
    - **Property 11: Bus operator filter correctness**
    - **Validates: Requirements 1.1-1.11**
  
  - [ ]* 5.4 Write unit tests for search edge cases
    - Test empty search results
    - Test search with no filters
    - Test search with all filters combined
    - _Requirements: 1.1-1.11, 15.1_

- [x] 6. Checkpoint - Ensure search functionality works
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Implement seat lock manager with Redis
  - [x] 7.1 Create SeatLockManager service
    - Implement acquireLock method with Redis SETNX
    - Implement releaseLock method
    - Implement isLocked method
    - Implement extendLock method
    - Set TTL to 10 minutes for locks
    - _Requirements: 3.1, 3.2, 3.3, 3.5_
  
  - [ ]* 7.2 Write property tests for seat locking
    - **Property 18: Lock creation on seat selection**
    - **Property 19: Locked seats are unavailable**
    - **Property 20: Payment converts lock to booking**
    - **Validates: Requirements 3.1, 3.2, 3.4**
  
  - [ ]* 7.3 Write unit tests for lock timeout
    - Test lock expiration after 10 minutes
    - Test lock release on payment
    - Test lock release on abandonment
    - _Requirements: 3.3, 3.5_

- [ ] 8. Implement seat selection service
  - [x] 8.1 Create SeatSelectionService
    - Implement getSeatLayout method
    - Implement selectSeat method with lock acquisition
    - Implement deselectSeat method with lock release
    - Parse seat layout JSON from bus configuration
    - Calculate available seats dynamically
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9_
  
  - [ ]* 8.2 Write property tests for seat selection
    - **Property 14: Seat layout structure correctness**
    - **Property 15: Seat status rendering correctness**
    - **Property 16: Seat selection updates price**
    - **Property 17: Seat deselection updates price**
    - **Property 18: Booked seats cannot be selected**
    - **Property 19: Fare summary calculation correctness**
    - **Property 20: Seat locking prevents concurrent selection**
    - **Validates: Requirements 2.1-2.9**
  
  - [ ]* 8.3 Write unit tests for seat selection edge cases
    - Test selecting already booked seat
    - Test concurrent seat selection
    - Test seat selection with expired lock
    - _Requirements: 2.7, 2.9_

- [ ] 9. Implement booking service
  - [x] 9.1 Create BookingService
    - Implement createBooking method with validation
    - Implement confirmBooking method
    - Implement cancelBooking method
    - Implement getBooking and getUserBookings methods
    - Generate unique PNR (10-character alphanumeric)
    - Calculate pricing with taxes and service fees
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 8.3, 8.4_
  
  - [ ]* 9.2 Write property tests for booking
    - **Property 24: Passenger details validation**
    - **Property 25: Passenger count must match seat count**
    - **Property 26: Fare summary completeness**
    - **Property 45: Cancellation releases seats**
    - **Property 46: Cancellation initiates refund**
    - **Validates: Requirements 4.2, 4.3, 4.4, 4.5, 8.3, 8.4**
  
  - [ ]* 9.3 Write unit tests for booking validation
    - Test booking with missing passenger details
    - Test booking with mismatched passenger/seat count
    - Test booking with expired lock
    - Test booking cancellation
    - _Requirements: 4.2, 4.3, 4.4, 8.3_

- [ ] 10. Checkpoint - Ensure booking flow works
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Implement payment service
  - [x] 11.1 Create PaymentService
    - Implement initiatePayment method
    - Implement processPayment method (with mock payment gateway)
    - Implement getPayment method
    - Implement refundPayment method
    - Add duplicate payment prevention
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_
  
  - [ ]* 11.2 Write property tests for payment
    - **Property 33: Payment initiation with correct amount**
    - **Property 34: Successful payment creates confirmed booking**
    - **Property 35: Failed payment releases lock**
    - **Property 36: Pending payment maintains lock**
    - **Property 37: Payment idempotency**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5**
  
  - [ ]* 11.3 Write unit tests for payment scenarios
    - Test payment with insufficient funds
    - Test payment timeout
    - Test duplicate payment attempt
    - Test refund processing
    - _Requirements: 6.3, 6.5_

- [ ] 12. Implement ticket generation service
  - [-] 12.1 Create TicketService
    - Implement generateTicket method
    - Implement getTicket method
    - Implement generatePDF method using iText or Apache PDFBox
    - Implement QR code generation using ZXing library
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  
  - [ ] 12.2 Implement email service
    - Create EmailService with JavaMailSender
    - Implement sendBookingConfirmation method
    - Implement sendPasswordReset method
    - Use async processing for email sending
    - _Requirements: 7.5, 5.3_
  
  - [ ]* 12.3 Write property tests for ticket generation
    - **Property 38: PNR uniqueness**
    - **Property 39: Ticket display completeness**
    - **Property 40: QR code round-trip**
    - **Property 41: PDF ticket completeness**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**
  
  - [ ]* 12.4 Write unit tests for ticket generation
    - Test ticket generation for confirmed booking
    - Test ticket generation fails for pending booking
    - Test PDF generation
    - Test email sending
    - _Requirements: 7.1, 7.2, 7.4, 7.5_

- [ ] 13. Implement user dashboard functionality
  - [ ] 13.1 Extend BookingService for dashboard
    - Implement getUpcomingTrips method
    - Implement getPastBookings method
    - Add filtering by date
    - Add sorting by departure time
    - _Requirements: 8.1, 8.2_
  
  - [ ] 13.2 Implement profile management
    - Create UserService with updateProfile method
    - Add profile validation
    - _Requirements: 8.5_
  
  - [ ]* 13.3 Write property tests for dashboard
    - **Property 43: Upcoming trips are future and sorted**
    - **Property 44: Past bookings are historical**
    - **Property 47: Profile update persistence**
    - **Validates: Requirements 8.1, 8.2, 8.5**

- [ ] 14. Implement time validation
  - [ ] 14.1 Add time validation to search and booking
    - Filter out past trips in search results
    - Reject bookings for past trips
    - Add scheduled job to close past trips
    - _Requirements: 9.1, 9.2, 9.3_
  
  - [ ]* 14.2 Write property tests for time validation
    - **Property 48: Past trips cannot be booked**
    - **Property 49: Search excludes past trips**
    - **Validates: Requirements 9.1, 9.2, 9.3**

- [ ] 15. Checkpoint - Ensure core user flow works end-to-end
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 16. Implement admin bus management
  - [ ] 16.1 Create AdminService for bus operations
    - Implement createBus method with admin authorization
    - Implement updateBus method with ownership check
    - Implement deleteBus method with future trip check
    - Implement getBusesByAdmin method
    - Implement assignBusAdmin method
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [ ]* 16.2 Write property tests for bus management
    - **Property 50: Valid bus creation**
    - **Property 51: Bus update propagation**
    - **Property 52: Deleted buses cannot be assigned**
    - **Property 53: Seat layout round-trip**
    - **Property 54: Bus display completeness**
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**
  
  - [ ]* 16.3 Write property tests for bus admin authorization
    - **Property 55: Bus admin can only manage own buses**
    - **Property 56: Bus admin assignment requires authorization**
    - **Property 57: Bus admin sees only their buses**
    - **Property 60: Cannot delete bus with future trips**
    - **Validates: Bus Admin Feature**

- [ ] 17. Implement admin route management
  - [ ] 17.1 Extend AdminService for route operations
    - Implement createCity method
    - Implement createRoute method with validation
    - Implement updateRoute method with future trip handling
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_
  
  - [ ]* 17.2 Write property tests for route management
    - **Property 61: City availability after creation**
    - **Property 59: Route creation validation**
    - **Property 60: Bus assignment referential integrity**
    - **Property 61: Schedule data round-trip**
    - **Property 62: Route updates affect future trips only**
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5**

- [ ] 18. Implement admin trip management
  - [ ] 18.1 Extend AdminService for trip operations
    - Implement createTrip method with admin authorization
    - Implement updateTrip method with ownership check
    - Implement setTripAvailability method
    - Implement getTripsByBusAdmin method
    - Calculate trip metrics (bookings, revenue, available seats)
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_
  
  - [ ]* 18.2 Write property tests for trip management
    - **Property 63: Trip price application**
    - **Property 64: Available days filtering**
    - **Property 65: Trip availability control**
    - **Property 66: Trip metrics calculation**
    - **Property 58: Bus admin sees only their trips**
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5**

- [ ] 19. Implement admin booking control
  - [ ] 19.1 Extend AdminService for booking operations
    - Implement getAllBookings with filters
    - Implement getBookingsByBusAdmin method
    - Implement cancelBookingAdmin method
    - Implement processRefund method
    - Add PNR search functionality
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_
  
  - [ ]* 19.2 Write property tests for booking control
    - **Property 67: Booking filter correctness**
    - **Property 68: Admin cancellation releases seats**
    - **Property 69: Refund processing creates record**
    - **Property 70: PNR search returns correct booking**
    - **Property 71: Booking display completeness**
    - **Property 59: Bus admin sees only their bookings**
    - **Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.5**

- [ ] 20. Implement data validation
  - [ ] 20.1 Add comprehensive validation
    - Create custom validators for email, phone, PNR format
    - Add @Valid annotations to controller methods
    - Implement global exception handler for validation errors
    - Add passenger count validation
    - Add payment amount verification
    - Add double booking prevention
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6_
  
  - [ ]* 20.2 Write property tests for validation
    - **Property 72: Required field validation**
    - **Property 73: Email format validation**
    - **Property 74: Phone format validation**
    - **Property 75: Payment amount verification**
    - **Property 76: Double booking prevention**
    - **Validates: Requirements 14.1, 14.2, 14.3, 14.5, 14.6**

- [ ] 21. Implement error handling
  - [ ] 21.1 Create global exception handler
    - Implement @ControllerAdvice for exception handling
    - Create custom exceptions (BookingException, PaymentException, ValidationException)
    - Return consistent error response format
    - Add error logging
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_
  
  - [ ]* 21.2 Write property tests for error handling
    - **Property 77: Payment failure includes error details**
    - **Property 78: Session expiration triggers cleanup**
    - **Property 79: Validation errors include field information**
    - **Validates: Requirements 15.2, 15.3, 15.5**

- [ ] 22. Checkpoint - Ensure admin functionality works
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 23. Implement REST API controllers
  - [ ] 23.1 Create SearchController
    - Implement GET /api/search/trips endpoint
    - Implement GET /api/search/cities endpoint
    - Implement GET /api/buses/{id} endpoint
    - Add request validation and error handling
    - _Requirements: 1.1-1.11_
  
  - [ ] 23.2 Create BookingController
    - Implement POST /api/bookings endpoint
    - Implement GET /api/bookings/{id} endpoint
    - Implement GET /api/bookings/user/{userId} endpoint
    - Implement DELETE /api/bookings/{id} endpoint
    - Add authentication checks
    - _Requirements: 4.1-4.6, 8.1-8.5_
  
  - [ ] 23.3 Create SeatController
    - Implement GET /api/trips/{id}/seats endpoint
    - Implement POST /api/trips/{id}/seats/select endpoint
    - Implement DELETE /api/trips/{id}/seats/deselect endpoint
    - _Requirements: 2.1-2.9_
  
  - [ ] 23.4 Create PaymentController
    - Implement POST /api/payments/initiate endpoint
    - Implement POST /api/payments/process endpoint
    - Implement GET /api/payments/{id} endpoint
    - Add payment webhook endpoint
    - _Requirements: 6.1-6.6_
  
  - [ ] 23.5 Create TicketController
    - Implement GET /api/tickets/{pnr} endpoint
    - Implement GET /api/tickets/{pnr}/pdf endpoint
    - Add authentication checks
    - _Requirements: 7.1-7.5_
  
  - [ ] 23.6 Create AuthController
    - Implement POST /api/auth/register endpoint
    - Implement POST /api/auth/login endpoint
    - Implement POST /api/auth/logout endpoint
    - Implement POST /api/auth/reset-password endpoint
    - _Requirements: 5.1-5.6_
  
  - [ ] 23.7 Create AdminController
    - Implement bus management endpoints (POST, PUT, DELETE /api/admin/buses)
    - Implement route management endpoints (POST, PUT /api/admin/routes)
    - Implement trip management endpoints (POST, PUT /api/admin/trips)
    - Implement booking management endpoints (GET, DELETE /api/admin/bookings)
    - Add role-based authorization (@PreAuthorize)
    - _Requirements: 10.1-13.5_
  
  - [ ]* 23.8 Write integration tests for API endpoints
    - Test complete booking flow via API
    - Test authentication flow
    - Test admin operations
    - Use MockMvc or RestAssured
    - _Requirements: All requirements_

- [ ] 24. Implement security configuration
  - [ ] 24.1 Configure Spring Security
    - Create SecurityConfig with JWT filter
    - Configure CORS for frontend
    - Add CSRF protection
    - Configure role-based access control
    - Add rate limiting with Bucket4j
    - _Requirements: Security considerations_
  
  - [ ] 24.2 Add security headers
    - Configure HTTPS enforcement
    - Add security headers (X-Frame-Options, X-Content-Type-Options)
    - Configure session management
    - _Requirements: Security considerations_

- [ ] 25. Implement background jobs
  - [ ] 25.1 Create scheduled tasks
    - Implement lock cleanup job (runs every minute)
    - Implement trip closure job (runs every hour)
    - Implement email retry job
    - Use @Scheduled annotation
    - _Requirements: 3.3, 9.1_
  
  - [ ] 25.2 Add async processing
    - Configure async executor
    - Make email sending async
    - Make PDF generation async
    - _Requirements: 7.4, 7.5_

- [ ] 26. Add caching strategy
  - [ ] 26.1 Configure Redis caching
    - Add @Cacheable for city suggestions
    - Add @Cacheable for bus details
    - Add @Cacheable for seat layouts
    - Configure cache TTL
    - _Requirements: Performance considerations_
  
  - [ ] 26.2 Implement cache invalidation
    - Invalidate bus cache on update
    - Invalidate seat cache on booking
    - Add cache warming on startup
    - _Requirements: Performance considerations_

- [ ] 27. Checkpoint - Ensure API works end-to-end
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 28. Create React frontend
  - [ ] 28.1 Set up React project
    - Create React app with TypeScript
    - Install dependencies (React Router, Axios, TanStack Query, Tailwind CSS)
    - Configure API client with Axios
    - Set up routing
    - _Requirements: Frontend_
  
  - [ ] 28.2 Implement search page
    - Create search form with city autocomplete
    - Create trip results list with filters
    - Create bus details modal
    - Add sorting options
    - _Requirements: 1.1-1.11_
  
  - [ ] 28.3 Implement seat selection page
    - Create seat layout grid component
    - Implement seat selection logic
    - Show fare summary
    - Add seat lock timer display
    - _Requirements: 2.1-2.9_
  
  - [ ] 28.4 Implement booking page
    - Create passenger details form
    - Show fare breakdown
    - Implement form validation
    - _Requirements: 4.1-4.6_
  
  - [ ] 28.5 Implement payment page
    - Create payment form (mock)
    - Show payment status
    - Handle payment success/failure
    - _Requirements: 6.1-6.6_
  
  - [ ] 28.6 Implement ticket page
    - Display ticket details
    - Show QR code
    - Add download PDF button
    - _Requirements: 7.1-7.5_
  
  - [ ] 28.7 Implement authentication pages
    - Create login page
    - Create registration page
    - Create password reset page
    - Add protected routes
    - _Requirements: 5.1-5.6_
  
  - [ ] 28.8 Implement user dashboard
    - Show upcoming trips
    - Show past bookings
    - Add cancel booking functionality
    - Add profile edit page
    - _Requirements: 8.1-8.5_
  
  - [ ] 28.9 Implement admin dashboard
    - Create bus management page
    - Create route management page
    - Create trip management page
    - Create booking management page
    - Add role-based navigation
    - _Requirements: 10.1-13.5_

- [ ] 29. Add monitoring and logging
  - [ ] 29.1 Configure application monitoring
    - Add Spring Boot Actuator
    - Configure health checks
    - Add metrics collection (Micrometer)
    - Configure Prometheus endpoints
    - _Requirements: Monitoring considerations_
  
  - [ ] 29.2 Add structured logging
    - Configure Logback with JSON format
    - Add correlation IDs for request tracing
    - Log all booking and payment events
    - Add error tracking (Sentry or similar)
    - _Requirements: Monitoring considerations_

- [ ] 30. Performance optimization
  - [ ] 30.1 Add database optimizations
    - Verify all indexes are created
    - Add database connection pooling (HikariCP)
    - Optimize N+1 queries with @EntityGraph
    - Add pagination for large result sets
    - _Requirements: Performance considerations_
  
  - [ ] 30.2 Add API optimizations
    - Implement response compression
    - Add ETag support for caching
    - Optimize JSON serialization
    - Add request/response logging
    - _Requirements: Performance considerations_

- [ ] 31. Final checkpoint - End-to-end testing
  - [ ] 31.1 Run all tests
    - Run all unit tests
    - Run all property-based tests (minimum 100 iterations each)
    - Run all integration tests
    - Verify test coverage >80%
    - _Requirements: All requirements_
  
  - [ ] 31.2 Manual testing
    - Test complete user booking flow
    - Test admin operations
    - Test error scenarios
    - Test on different browsers
    - _Requirements: All requirements_

- [ ] 32. Documentation and deployment preparation
  - [ ] 32.1 Create API documentation
    - Add Swagger/OpenAPI annotations
    - Generate API documentation
    - Create Postman collection
    - _Requirements: Documentation_
  
  - [ ] 32.2 Create deployment artifacts
    - Create Dockerfile for backend
    - Create Dockerfile for frontend
    - Create docker-compose.yml for local development
    - Create Kubernetes manifests (optional)
    - Add environment variable documentation
    - _Requirements: Deployment considerations_
  
  - [ ] 32.3 Create README and setup guide
    - Document prerequisites
    - Document setup steps
    - Document running tests
    - Document deployment process
    - _Requirements: Documentation_

## Notes

- Tasks marked with `*` are optional property-based and unit tests that can be skipped for faster MVP
- Each property test should run minimum 100 iterations
- Property tests should use jqwik library for Java
- All property tests must reference their design document property number in comments
- Checkpoints ensure incremental validation and provide opportunities for user feedback
- The implementation follows the recommended build order: search → seat selection → booking → payment → tickets → admin
- Focus on getting core functionality working before adding advanced features
