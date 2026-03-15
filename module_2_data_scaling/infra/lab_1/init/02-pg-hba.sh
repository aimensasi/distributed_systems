#!/bin/bash
echo "host replication replicator 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT pg_reload_conf();"