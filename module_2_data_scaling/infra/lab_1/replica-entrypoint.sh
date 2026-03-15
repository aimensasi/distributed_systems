#!/bin/bash
set -e

PGDATA="/var/lib/postgresql/data"

# If data directory already has a cluster, just start PostgreSQL
# This handles container restarts — we don't want to re-clone on every restart
if [ -f "$PGDATA/PG_VERSION" ]; then
  echo "Replica data directory already exists, starting PostgreSQL..."
  exec docker-entrypoint.sh postgres \
    -c primary_conninfo="host=${PRIMARY_HOST} port=${PRIMARY_PORT} user=${REPLICATION_USER} password=${REPLICATION_PASSWORD}" \
    -c hot_standby=on \
    -c recovery_min_apply_delay=200
fi

echo "No data directory found — bootstrapping replica from primary..."

# Clear the data directory (Docker creates it as a volume mount point)
rm -rf "$PGDATA"/*

# Clone the primary
PGPASSWORD=$REPLICATION_PASSWORD pg_basebackup \
  -h "$PRIMARY_HOST" \
  -p "$PRIMARY_PORT" \
  -U "$REPLICATION_USER" \
  -D "$PGDATA" \
  -Fp \
  -Xs \
  -P \
  -R

# -Fp  = plain format (files, not tar)
# -Xs  = stream WAL during backup (no gap between backup end and replication start)
# -P   = show progress
# -R   = write recovery config (creates standby.signal + postgresql.auto.conf with primary_conninfo)

echo "pg_basebackup complete. Starting replica..."

# Start PostgreSQL in hot standby mode (allows read queries on replica)
exec docker-entrypoint.sh postgres \
  -c hot_standby=on \
  -c recovery_min_apply_delay=200