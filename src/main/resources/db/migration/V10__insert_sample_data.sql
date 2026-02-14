-- Sample data for development and testing
-- This migration is optional and can be skipped in production

-- Insert sample cities
INSERT INTO cities (id, name, state) VALUES
('c1', 'Mumbai', 'Maharashtra'),
('c2', 'Delhi', 'Delhi'),
('c3', 'Bangalore', 'Karnataka'),
('c4', 'Pune', 'Maharashtra'),
('c5', 'Hyderabad', 'Telangana');

-- Insert sample users (password: 'password123' hashed with BCrypt)
-- Note: In production, use proper password hashing
INSERT INTO users (id, email, password_hash, name, phone, is_email_verified, role) VALUES
('u1', 'admin@busticket.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', '9876543210', true, 'ADMIN'),
('u2', 'busadmin@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bus Admin', '9876543211', true, 'BUS_ADMIN'),
('u3', 'user@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John Doe', '9876543212', true, 'USER');

-- Insert sample buses
INSERT INTO buses (id, company_name, bus_number, bus_type, total_seats, seat_layout, amenities, admin_user_id) VALUES
('b1', 'Express Travels', 'MH-01-AB-1234', 'AC', 40, 
 '{"rows": 10, "columns": 4, "seats": [{"number": "A1", "row": 1, "column": 1}, {"number": "A2", "row": 1, "column": 2}]}',
 '["WiFi", "Charging Port", "Water Bottle", "Blanket"]', 'u2'),
('b2', 'Comfort Coaches', 'DL-02-CD-5678', 'SLEEPER', 30,
 '{"rows": 10, "columns": 3, "seats": [{"number": "L1", "row": 1, "column": 1}, {"number": "L2", "row": 1, "column": 2}]}',
 '["WiFi", "Blanket", "Pillow"]', 'u2');

-- Insert sample routes
INSERT INTO routes (id, departure_city_id, destination_city_id, distance, estimated_duration) VALUES
('r1', 'c1', 'c2', 1400.00, 1440),  -- Mumbai to Delhi (24 hours)
('r2', 'c1', 'c4', 150.00, 180),     -- Mumbai to Pune (3 hours)
('r3', 'c3', 'c5', 570.00, 600);     -- Bangalore to Hyderabad (10 hours)

-- Insert sample trips (future dates)
INSERT INTO trips (id, route_id, bus_id, departure_time, arrival_time, price, is_open, operating_days) VALUES
('t1', 'r1', 'b1', CURRENT_TIMESTAMP + INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '3 days', 1500.00, true, '["MONDAY", "WEDNESDAY", "FRIDAY"]'),
('t2', 'r2', 'b2', CURRENT_TIMESTAMP + INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '1 day 3 hours', 500.00, true, '["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]');

-- Note: Bookings, payments, refunds, and ratings are not inserted as sample data
-- These will be created through the application during normal operation
