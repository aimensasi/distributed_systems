CREATE TABLE IF NOT EXISTS counters (
    id   SERIAL PRIMARY KEY,
    value INT NOT NULL DEFAULT 0
);

INSERT INTO counters (id, value)
VALUES (1, 0)
    ON CONFLICT (id) DO NOTHING;


CREATE TABLE IF NOT EXISTS accounts (
    id   SERIAL PRIMARY KEY,
    balance INT NOT NULL DEFAULT 0
);

INSERT INTO accounts (id, balance)
VALUES (1, 100)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO accounts (id, balance)
VALUES (2, 100)
    ON CONFLICT (id) DO NOTHING;