CREATE TABLE saga_state (
    id SERIAL PRIMARY KEY,
    orderId INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    step VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);