-- Create buses table
CREATE TABLE buses (
    id VARCHAR(36) PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    bus_number VARCHAR(50) NOT NULL UNIQUE,
    bus_type VARCHAR(20) NOT NULL CHECK (bus_type IN ('AC', 'NON_AC', 'SLEEPER', 'VIP')),
    total_seats INTEGER NOT NULL,
    seat_layout JSONB NOT NULL,
    amenities JSONB,
    admin_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_buses_admin_user FOREIGN KEY (admin_user_id) REFERENCES users(id)
);

-- Create indexes for bus queries
CREATE INDEX idx_buses_company_name ON buses(company_name);
CREATE INDEX idx_buses_admin_user_id ON buses(admin_user_id);
CREATE INDEX idx_buses_bus_number ON buses(bus_number);
