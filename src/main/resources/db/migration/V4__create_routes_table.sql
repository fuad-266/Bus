-- Create routes table
CREATE TABLE routes (
    id VARCHAR(36) PRIMARY KEY,
    departure_city_id VARCHAR(36) NOT NULL,
    destination_city_id VARCHAR(36) NOT NULL,
    distance DECIMAL(10, 2),
    estimated_duration INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (departure_city_id) REFERENCES cities(id),
    FOREIGN KEY (destination_city_id) REFERENCES cities(id)
);

-- Create indexes for route queries
CREATE INDEX idx_routes_departure_city ON routes(departure_city_id);
CREATE INDEX idx_routes_destination_city ON routes(destination_city_id);
CREATE INDEX idx_routes_cities ON routes(departure_city_id, destination_city_id);
