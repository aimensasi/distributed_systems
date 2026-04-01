DROP TABLE IF EXISTS key_value;
CREATE TABLE key_value (
   key   VARCHAR PRIMARY KEY,
   value INT NOT NULL
);


CREATE TABLE IF NOT EXISTS metrics (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    metric   DECIMAL(10, 2) NOT NULL ,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS metric_created_at ON metrics (created_at);

CREATE TABLE IF NOT EXISTS metrics_partition (
     id INT GENERATED ALWAYS AS IDENTITY,
     metric   DECIMAL(10, 2) NOT NULL ,
    created_at TIMESTAMP,

    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);


