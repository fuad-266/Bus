# Migration Validation Checklist

## Pre-Deployment Validation

This checklist ensures all database migrations are correct before deployment.

### ✅ 1. Migration Files Created

- [x] V1__create_cities_table.sql
- [x] V2__create_users_table.sql
- [x] V3__create_buses_table.sql
- [x] V4__create_routes_table.sql
- [x] V5__create_trips_table.sql
- [x] V6__create_bookings_table.sql
- [x] V7__create_payments_table.sql
- [x] V8__create_refunds_table.sql
- [x] V9__create_trip_ratings_table.sql
- [x] V10__insert_sample_data.sql (optional)

### ✅ 2. Naming Convention

All migration files follow Flyway naming convention:
- Format: `V{version}__{description}.sql`
- Sequential versioning: V1, V2, V3, ..., V10
- Double underscore separator
- Descriptive names

### ✅ 3. SQL Syntax Validation

All SQL statements use PostgreSQL-compatible syntax:
- [x] CREATE TABLE statements
- [x] PRIMARY KEY constraints
- [x] FOREIGN KEY constraints with REFERENCES
- [x] CHECK constraints for enums
- [x] UNIQUE constraints
- [x] NOT NULL constraints
- [x] DEFAULT values
- [x] JSONB data type (PostgreSQL-specific)
- [x] CREATE INDEX statements
- [x] VARCHAR, INTEGER, DECIMAL, TIMESTAMP, BOOLEAN types

### ✅ 4. Table Dependencies

Tables are created in correct order (parent before child):
1. cities (no dependencies)
2. users (no dependencies)
3. buses (depends on users)
4. routes (depends on cities)
5. trips (depends on routes, buses)
6. bookings (depends on trips, users)
7. payments (depends on bookings)
8. refunds (depends on payments, bookings)
9. trip_ratings (depends on trips, buses, users)

### ✅ 5. Foreign Key Relationships

All foreign keys reference existing tables:
- [x] buses.admin_user_id → users.id
- [x] routes.departure_city_id → cities.id
- [x] routes.destination_city_id → cities.id
- [x] trips.route_id → routes.id
- [x] trips.bus_id → buses.id
- [x] bookings.trip_id → trips.id
- [x] bookings.user_id → users.id
- [x] payments.booking_id → bookings.id
- [x] refunds.payment_id → payments.id
- [x] refunds.booking_id → bookings.id
- [x] trip_ratings.trip_id → trips.id
- [x] trip_ratings.bus_id → buses.id
- [x] trip_ratings.user_id → users.id

### ✅ 6. Primary Keys

All tables have primary keys defined:
- [x] cities(id)
- [x] users(id)
- [x] buses(id)
- [x] routes(id)
- [x] trips(id)
- [x] bookings(id)
- [x] payments(id)
- [x] refunds(id)
- [x] trip_ratings(id)

### ✅ 7. Unique Constraints

Unique constraints on appropriate columns:
- [x] cities.name
- [x] users.email
- [x] buses.bus_number
- [x] bookings.pnr
- [x] trip_ratings(trip_id, user_id) - composite

### ✅ 8. Check Constraints

Enum-like constraints for data integrity:
- [x] users.role IN ('USER', 'ADMIN', 'BUS_ADMIN')
- [x] buses.bus_type IN ('AC', 'NON_AC', 'SLEEPER', 'VIP')
- [x] bookings.status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'FAILED')
- [x] payments.status IN ('PENDING', 'SUCCESS', 'FAILED')
- [x] refunds.status IN ('PENDING', 'COMPLETED', 'FAILED')
- [x] trip_ratings.rating >= 1.0 AND rating <= 5.0

### ✅ 9. Indexes Created

Performance indexes on frequently queried columns:
- [x] cities: name
- [x] users: email
- [x] buses: company_name, admin_user_id, bus_number
- [x] routes: departure_city_id, destination_city_id, composite
- [x] trips: departure_time, route_id + departure_time, bus_id, is_open
- [x] bookings: pnr, user_id, trip_id, status, created_at
- [x] payments: booking_id, status, transaction_id
- [x] refunds: payment_id, booking_id, status
- [x] trip_ratings: bus_id, trip_id, user_id, unique composite

### ✅ 10. Data Types

Appropriate data types for each column:
- [x] IDs: VARCHAR(36) for UUIDs
- [x] Emails: VARCHAR(255)
- [x] Names: VARCHAR(100)
- [x] Prices/Amounts: DECIMAL(10, 2)
- [x] Ratings: DECIMAL(2, 1)
- [x] Timestamps: TIMESTAMP
- [x] Booleans: BOOLEAN
- [x] JSON data: JSONB
- [x] Text fields: TEXT

### ✅ 11. NOT NULL Constraints

Required fields have NOT NULL constraints:
- [x] All primary keys
- [x] All foreign keys (except nullable user_id in bookings)
- [x] Essential business fields (name, email, price, etc.)

### ✅ 12. Default Values

Appropriate default values:
- [x] created_at: CURRENT_TIMESTAMP
- [x] is_email_verified: FALSE
- [x] role: 'USER'
- [x] is_open: TRUE

### ✅ 13. JSONB Fields

JSONB fields for flexible data:
- [x] buses.seat_layout - seat configuration
- [x] buses.amenities - array of amenities
- [x] bookings.seat_numbers - array of seat numbers
- [x] bookings.passengers - array of passenger objects
- [x] trips.operating_days - array of day names

### ✅ 14. Design Document Compliance

All requirements from design.md implemented:
- [x] All 9 tables created
- [x] All foreign keys as specified
- [x] All indexes as specified
- [x] All constraints as specified
- [x] JSONB fields as specified
- [x] Proper data types as specified

### ✅ 15. Configuration Files

Flyway configuration in place:
- [x] application-dev.properties has Flyway settings
- [x] spring.flyway.enabled=true
- [x] spring.flyway.baseline-on-migrate=true
- [x] spring.flyway.locations=classpath:db/migration
- [x] Database connection configured

### ✅ 16. Documentation

Complete documentation provided:
- [x] MIGRATION_TESTING.md - Testing procedures
- [x] MIGRATION_SUMMARY.md - Schema overview
- [x] MIGRATION_VALIDATION_CHECKLIST.md - This file
- [x] docker-compose.yml - Local development setup

## Manual Testing Required

The following tests should be performed when PostgreSQL is available:

### Test 1: Migration Execution
```bash
docker-compose up -d
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
```
Expected: All 10 migrations apply successfully

### Test 2: Table Creation
```sql
\dt
```
Expected: 9 tables listed (cities, users, buses, routes, trips, bookings, payments, refunds, trip_ratings)

### Test 3: Foreign Key Constraints
```sql
-- Should fail (user doesn't exist)
INSERT INTO buses (id, company_name, bus_number, bus_type, total_seats, seat_layout, admin_user_id)
VALUES ('test', 'Test', 'TEST001', 'AC', 40, '{}', 'nonexistent');
```
Expected: Foreign key constraint violation error

### Test 4: Check Constraints
```sql
-- Should fail (invalid role)
INSERT INTO users (id, email, password_hash, name, phone, role)
VALUES ('test', 'test@test.com', 'hash', 'Test', '1234567890', 'INVALID');
```
Expected: Check constraint violation error

### Test 5: Unique Constraints
```sql
-- Should fail (duplicate email)
INSERT INTO users (id, email, password_hash, name, phone) VALUES
('id1', 'test@test.com', 'hash', 'Test1', '1234567890');
INSERT INTO users (id, email, password_hash, name, phone) VALUES
('id2', 'test@test.com', 'hash', 'Test2', '1234567890');
```
Expected: Unique constraint violation error on second insert

### Test 6: Index Usage
```sql
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@test.com';
```
Expected: Index scan on idx_users_email

### Test 7: JSONB Operations
```sql
-- Insert bus with JSONB data
INSERT INTO users (id, email, password_hash, name, phone, role) VALUES
('admin1', 'admin@test.com', 'hash', 'Admin', '1234567890', 'BUS_ADMIN');

INSERT INTO buses (id, company_name, bus_number, bus_type, total_seats, seat_layout, amenities, admin_user_id)
VALUES ('bus1', 'Test Co', 'TEST001', 'AC', 40, 
        '{"rows": 10, "columns": 4}',
        '["WiFi", "AC"]',
        'admin1');

-- Query JSONB
SELECT amenities FROM buses WHERE id = 'bus1';
```
Expected: JSONB data stored and retrieved correctly

### Test 8: Sample Data
```sql
SELECT COUNT(*) FROM cities;
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM buses;
```
Expected: 5 cities, 3 users, 2 buses (from V10 migration)

## Sign-Off

- [x] All migration files created
- [x] All SQL syntax validated
- [x] All constraints implemented
- [x] All indexes created
- [x] All foreign keys defined
- [x] Documentation complete
- [x] Configuration files ready
- [x] Docker setup provided

**Status**: ✅ READY FOR TESTING

**Note**: Actual database testing requires:
1. PostgreSQL 15+ running (via Docker or local install)
2. Java 17+ installed
3. Maven installed or use ./mvnw wrapper

Once these prerequisites are met, run the tests in MIGRATION_TESTING.md to verify the migrations work correctly.
