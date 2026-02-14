-- Create trips table
CREATE TABLE trips (
    id VARCHAR(36) PRIMARY KEY,
    route_id VARCHAR(36) NOT NULL,
    bus_id VARCHAR(36) NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    is_open BOOLEAN DEFAULT TRUE,
    operating_days JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (route_id) REFERENCES routes(id),
    FOREIGN KEY (bus_id) REFERENCES buses(id)
);

-- Create indexes for trip queries
CREATE INDEX idx_trips_departure_time ON trips(departure_time);
CREATE INDEX idx_trips_route_date ON trips(route_id, departure_time);
CREATE INDEX idx_trips_bus_id ON trips(bus_id);
CREATE INDEX idx_trips_is_open ON trips(is_open);
