# Database Migration Testing Guide

## Overview

This document describes how to test the Flyway database migrations for the Bus Ticket Booking System.

## Prerequisites

- Docker and Docker Compose installed
- Java 17+ installed
- Maven installed

## Migration Files Created

The following Flyway migration scripts have been created in `src/main/resources/db/migration/`:

1. **V1__create_cities_table.sql** - Cities table with name index
2. **V2__create_users_table.sql** - Users table with email index and role constraint
3. **V3__create_buses_table.sql** - Buses table with foreign key to users, indexes on company_name and admin_user_id
4. **V4__create_routes_table.sql** - Routes table with foreign keys to cities, composite index on city pairs
5. **V5__create_trips_table.sql** - Trips table with foreign keys to routes and buses, indexes on departure_time and route_date
6. **V6__create_bookings_table.sql** - Bookings table with foreign keys to trips and users, indexes on pnr, user_id, trip_id, and status
7. **V7__create_payments_table.sql** - Payments table with foreign key to bookings, indexes on booking_id and status
8. **V8__create_refunds_table.sql** - Refunds table with foreign keys to payments and bookings
9. **V9__create_trip_ratings_table.sql** - Trip ratings table with foreign keys and unique constraint on trip_id + user_id

## Database Schema Features

### Tables Created
- `cities` - City master data
- `users` - User accounts with role-based access (USER, ADMIN, BUS_ADMIN)
- `buses` - Bus inventory with JSONB seat layout and amenities
- `routes` - Routes between cities
- `trips` - Scheduled trips with pricing and availability
- `bookings` - Ticket bookings with passenger details (JSONB)
- `payments` - Payment transactions
- `refunds` - Refund records
- `trip_ratings` - User ratings for trips

### Constraints Implemented
- Primary keys on all tables (VARCHAR(36) for UUIDs)
- Foreign key constraints for referential integrity
- CHECK constraints for enums (user role, bus type, booking status, payment status, refund status)
- CHECK constraint for rating range (1.0 to 5.0)
- UNIQUE constraints (city name, bus number, PNR, email, trip+user rating)
- NOT NULL constraints on required fields

### Indexes Created
- **cities**: name
- **users**: email
- **buses**: company_name, admin_user_id, bus_number
- **routes**: departure_city_id, destination_city_id, composite (departure + destination)
- **trips**: departure_time, route_id + departure_time, bus_id, is_open
- **bookings**: pnr, user_id, trip_id, status, created_at
- **payments**: booking_id, status, transaction_id
- **refunds**: payment_id, booking_id, status
- **trip_ratings**: bus_id, trip_id, user_id, unique (trip_id + user_id)

## Testing Steps

### 1. Start PostgreSQL and Redis

```bash
docker-compose up -d
```

This will start:
- PostgreSQL 15 on port 5432
- Redis 7 on port 6379

### 2. Verify Services are Running

```bash
docker-compose ps
```

Both services should show as "Up" and healthy.

### 3. Run Flyway Migrations via Spring Boot

```bash
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will:
1. Connect to PostgreSQL
2. Run Flyway migrations automatically
3. Create all tables with indexes and constraints
4. Validate the schema

### 4. Verify Migration Success

Check the application logs for:
```
Flyway Community Edition ... by Redgate
Database: jdbc:postgresql://localhost:5432/bus_ticket_dev (PostgreSQL 15.x)
Successfully validated 9 migrations
Creating Schema History table "public"."flyway_schema_history" ...
Current version of schema "public": << Empty Schema >>
Migrating schema "public" to version "1 - create cities table"
Migrating schema "public" to version "2 - create users table"
...
Migrating schema "public" to version "9 - create trip ratings table"
Successfully applied 9 migrations to schema "public"
```

### 5. Verify Tables in Database

Connect to PostgreSQL:
```bash
docker exec -it bus-ticket-postgres psql -U postgres -d bus_ticket_dev
```

Run verification queries:
```sql
-- List all tables
\dt

-- Check cities table
\d cities

-- Check users table with constraints
\d users

-- Check buses table with foreign keys
\d buses

-- Check all indexes
\di

-- Verify flyway_schema_history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### 6. Test Constraints

Test that constraints are working:

```sql
-- Test user role constraint (should fail)
INSERT INTO users (id, email, password_hash, name, phone, role) 
VALUES ('test-id', 'test@test.com', 'hash', 'Test', '1234567890', 'INVALID_ROLE');

-- Test rating constraint (should fail)
INSERT INTO trip_ratings (id, trip_id, bus_id, user_id, rating) 
VALUES ('test-id', 'trip-id', 'bus-id', 'user-id', 6.0);

-- Test unique email constraint (should fail on second insert)
INSERT INTO users (id, email, password_hash, name, phone) 
VALUES ('id1', 'unique@test.com', 'hash', 'Test', '1234567890');
INSERT INTO users (id, email, password_hash, name, phone) 
VALUES ('id2', 'unique@test.com', 'hash', 'Test', '1234567890');
```

### 7. Test Foreign Key Constraints

```sql
-- Test bus foreign key to users (should fail - user doesn't exist)
INSERT INTO buses (id, company_name, bus_number, bus_type, total_seats, seat_layout, admin_user_id)
VALUES ('bus-id', 'Test Company', 'BUS001', 'AC', 40, '{}', 'nonexistent-user-id');

-- Create user first, then bus should succeed
INSERT INTO users (id, email, password_hash, name, phone, role) 
VALUES ('admin-id', 'admin@test.com', 'hash', 'Admin', '1234567890', 'BUS_ADMIN');

INSERT INTO buses (id, company_name, bus_number, bus_type, total_seats, seat_layout, admin_user_id)
VALUES ('bus-id', 'Test Company', 'BUS001', 'AC', 40, '{}', 'admin-id');
```

### 8. Test Indexes

Verify indexes are being used:

```sql
-- Explain query plans to verify index usage
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@test.com';
EXPLAIN ANALYZE SELECT * FROM bookings WHERE pnr = 'ABC1234567';
EXPLAIN ANALYZE SELECT * FROM trips WHERE departure_time > NOW() AND route_id = 'route-id';
```

### 9. Clean Up

Stop and remove containers:
```bash
docker-compose down
```

To also remove volumes (database data):
```bash
docker-compose down -v
```

## Migration Rollback

Flyway doesn't support automatic rollback. To rollback:

1. Drop the database:
```bash
docker exec -it bus-ticket-postgres psql -U postgres -c "DROP DATABASE bus_ticket_dev;"
docker exec -it bus-ticket-postgres psql -U postgres -c "CREATE DATABASE bus_ticket_dev;"
```

2. Re-run migrations:
```bash
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
```

## Common Issues

### Issue: Flyway validation fails
**Solution**: Check that no manual changes were made to the database. Drop and recreate the database.

### Issue: Foreign key constraint violation
**Solution**: Ensure data is inserted in the correct order (parent tables before child tables).

### Issue: Connection refused to PostgreSQL
**Solution**: Ensure Docker containers are running with `docker-compose ps`.

### Issue: Migration checksum mismatch
**Solution**: Don't modify migration files after they've been applied. Create a new migration instead.

## Next Steps

After successful migration testing:
1. Create JPA entities matching the database schema (Task 3)
2. Implement repositories with Spring Data JPA
3. Add sample data for development/testing
4. Configure staging and production database connections
