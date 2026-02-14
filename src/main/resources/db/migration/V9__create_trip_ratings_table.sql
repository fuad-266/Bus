-- Create trip_ratings table
CREATE TABLE trip_ratings (
    id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    bus_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    rating DECIMAL(2, 1) NOT NULL CHECK (rating >= 1.0 AND rating <= 5.0),
    review TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trip_ratings_trip FOREIGN KEY (trip_id) REFERENCES trips(id),
    CONSTRAINT fk_trip_ratings_bus FOREIGN KEY (bus_id) REFERENCES buses(id),
    CONSTRAINT fk_trip_ratings_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create indexes for rating queries
CREATE INDEX idx_trip_ratings_bus_id ON trip_ratings(bus_id);
CREATE INDEX idx_trip_ratings_trip_id ON trip_ratings(trip_id);
CREATE INDEX idx_trip_ratings_user_id ON trip_ratings(user_id);

-- Create unique constraint to prevent duplicate ratings
CREATE UNIQUE INDEX idx_trip_ratings_unique ON trip_ratings(trip_id, user_id);
