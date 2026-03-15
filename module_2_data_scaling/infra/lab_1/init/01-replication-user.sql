-- Creates the replication user the replica will authenticate as
CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'replicator_pass';

-- Allow the replication user to connect from any host on the Docker network
-- pg_hba.conf entries can be injected via SQL in PostgreSQL 16
SELECT pg_reload_conf();