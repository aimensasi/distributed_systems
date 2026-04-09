# Module 2: Data Layer Scaling — Reference Sheet

---

## Lab 2.1 — Read Replicas & Replication Lag

**The problem:** All reads hitting the primary creates a read bottleneck.

**The fix:** Route reads to replicas, writes to primary.

**The catch:** Replicas are always behind the primary (async replication). A write that succeeds on the primary may not be visible on the replica immediately.

**Replication lag check (PostgreSQL):**
```sql
-- On replica: how far behind is it?
SELECT now() - pg_last_xact_replay_timestamp() AS lag;

-- On replica: has it replayed past a given LSN?
SELECT pg_last_wal_replay_lsn() >= $1::pg_lsn;

-- On primary: current WAL position
SELECT pg_current_wal_lsn();
```

**Read-your-writes consistency — the production pattern:**
```
Write commits on primary
  → store LSN in Redis: SET wal:{key} {lsn} EX 5
Read arrives
  → check Redis for LSN
  → if exists: check replica replay position against LSN
      → caught up? serve from replica, delete key
      → behind? serve from primary
  → if not exists: serve from replica
```

**Routing strategies (tradeoffs):**
```
@Transactional(readOnly=true) as routing signal → hack, implicit, fragile
Explicit @ReadOnly annotation                   → clear intent, requires code change
Repository separation (ReadRepo / WriteRepo)    → cleanest, type-safe
AbstractRoutingDataSource                       → transparent to app code, one-time setup
```

**DataSourceBuilder vs HikariConfig:**
```java
// DataSourceBuilder — IGNORES spring.datasource.hikari.* properties
// Use only for simple cases where pool config doesn't matter

// HikariConfig — explicit, always works regardless of autoconfiguration
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(5);
config.setConnectionTimeout(3000);
return new HikariDataSource(config);
```

**Spring Boot autoconfiguration rule:**
```
No @Bean DataSource defined  → Spring reads spring.datasource.hikari.* from properties
@Bean DataSource defined     → Spring backs off, hikari.* properties are IGNORED
```

---

## Lab 2.2 — Connection Pool Exhaustion

**The problem:** A shared connection pool means slow queries can starve fast ones. At scale, N app instances × pool size = too many DB connections.

**Pool exhaustion failure mode:**
```
Report endpoint holds all 5 connections for 5s
→ health check endpoint can't get a connection
→ load balancer marks instance unhealthy
→ cascading failure from a healthy database
```

**Bulkhead pattern — separate pools per concern:**
```java
@Bean @Primary
public DataSource mainDataSource() {
    // pool size 5, timeout 3s — user-facing requests
}

@Bean
public DataSource reportDataSource() {
    // pool size 3, timeout 10s — slow report queries
}
```

**PgBouncer — connection multiplexing:**
```
Without PgBouncer: 3 instances × 8 connections = 24 DB connections at rest
With PgBouncer:    all instances → PgBouncer → 5 real DB connections

pool_mode = transaction  → connection returned to pool after each transaction
                         → most efficient, but @Transactional behavior changes
```

**PgBouncer is not a replacement for HikariCP:**
```
HikariCP   → reuses connections within one app instance
PgBouncer  → caps total connections across all app instances
Both together → HikariCP reuses → PgBouncer multiplexes
```

**Connection timeout guidance:**
```
User-facing endpoints  → 1-3s   (fail fast, bad UX to wait)
Background jobs        → 30-60s (no user waiting)
Report/batch queries   → 5-15s  (somewhere in between)
```

**Thread pool sizing rule:**
```
CPU-bound work:  pool size ≈ CPU cores
I/O-bound work:  pool size = cores × (1 + wait_time / compute_time)
                 (DB queries are I/O-bound — pool should be larger than core count)
```

---

## Lab 2.3 — Table Partitioning

**The problem:** A 50M row table with scattered data causes slow queries even with indexes, and bulk deletes are painful.

**Partition pruning in action:**
```sql
-- Unpartitioned 10M rows, 7-day filter:   25,794ms
-- Partitioned monthly, same filter:          208ms
-- Speedup: 124x

-- Key log line that proves pruning worked:
-- Subplans Removed: 24   ← 24 of 25 partitions skipped entirely
```

**Partitioned table schema (PostgreSQL):**
```sql
-- Partition key must be in primary key
CREATE TABLE metrics (
    id INT GENERATED ALWAYS AS IDENTITY,
    value DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Monthly partition
CREATE TABLE metrics_y2026m03
    PARTITION OF metrics
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
```

**Operational win — DROP vs DELETE:**
```
DELETE FROM metrics WHERE created_at < '2024-04-01'  → 3,136ms (row-by-row)
DROP TABLE metrics_y2024m03                          → ~1ms   (metadata only)
```

**When to partition:**
```
✓ Table > 10M rows AND queries filter by the partition key
✓ Need to bulk-delete old data regularly (even at 5M rows)
✓ Time-series data — natural monthly/daily boundaries
✗ Queries don't filter by partition key (no pruning, just overhead)
✗ Table has foreign keys pointing to it from other tables
✗ Table is small — index is sufficient
```

**Partition key must be in primary key** — PostgreSQL enforces this. Foreign keys from other tables become awkward. Design for this upfront.

**pg_partman** — extension that auto-creates future partitions and drops old ones. Essential for production time-series tables.

---

## Lab 2.4 — Application-Level Sharding

**The problem:** Write bottleneck. Read replicas only scale reads. All writes still go to one primary.

**Modulo routing:**
```java
int shard = userId % numberOfShards;  // simple but breaks on topology change
```

**Cross-shard query cost:**
```java
// What used to be one SQL query
SELECT * FROM orders WHERE amount > 50;

// Becomes this — every time
for (JdbcTemplate shard : allShards) {
    results.addAll(shard.query(...));  // N network round trips
}
// + application-level merge, no ordering guarantee
```

**Parallel cross-shard with CompletableFuture:**
```java
// Sequential: latency = shard1 + shard2 + shard3
// Parallel:   latency = max(shard1, shard2, shard3)

List<CompletableFuture<List<T>>> futures = shards.stream()
    .map(shard -> CompletableFuture.supplyAsync(
        () -> shard.query(...), dedicatedExecutor))
    .toList();

return futures.stream()
    .map(CompletableFuture::join)
    .flatMap(List::stream)
    .toList();
```

**Hot shard problem:**
```
userId % 3 = 1 for a high-traffic user
→ all their requests hit shard 1
→ shard 1 runs hot, shard 0 and 2 sit idle
→ adding more shards doesn't help — same user still hashes to one shard
```

**Global unique IDs across shards:**
```
Auto-increment per shard → duplicate IDs across shards
Snowflake ID             → 64-bit: timestamp + machine_id + sequence
UUID                     → globally unique, no coordination needed
```

**Shard key selection rule:**
```
Shard by X when:
  → most queries filter by X  (avoids scatter-gather)
  → related data hashes to same shard  (avoids cross-shard joins)
  → X is not a single hot entity  (avoids hot shard)

Common good keys: user_id, tenant_id, workspace_id
Common bad keys:  created_at (time-based hotspot), status (low cardinality)
```

**Logical vs physical shards (Notion pattern):**
```
Logical shard  → stable unit of data ownership (e.g. schema042)
Physical shard → actual DB server (can change)
Routing table  → maps logical → physical (only thing that changes on rebalance)

480 logical shards / 32 physical DBs
  → move shard by updating routing table, no rehashing
  → 480 chosen for many factors (divisible by 2,3,4,5,6,8,10,12,15,16,20,24,30,32,40,48...)
```

---

## Lab 2.5 — Consistent Hashing

**The problem:** Modulo routing remaps ~75% of keys when adding one shard.

**Formula for modulo redistribution:**
```
keys remapped = 1 - (old_shards / new_shards)
3 → 4 shards:  1 - (3/4) = 75% remapped
```

**Consistent hashing guarantee:**
```
Adding 1 node to N-node ring: only 1/(N+1) keys move
3 → 4 shards: only ~25% remapped (minimum theoretically possible)
```

**Hash ring implementation:**
```java
TreeMap<Integer, String> ring;  // position → nodeId

// Add node: place N virtual nodes at hash positions
for (int i = 1; i <= VIRTUAL_NODES; i++) {
    int pos = hash(nodeId + "-" + i);
    ring.put(pos, nodeId);
}

// Route key: find nearest clockwise node
String getNode(String key) {
    int pos = hash(key);
    Integer found = ring.ceilingKey(pos);
    if (found == null) return ring.firstEntry().getValue(); // wraparound
    return ring.get(found);
}
```

**Virtual nodes tradeoff:**
```
7 virtual nodes:   distribution imbalance 2.3x, remapped 18%
150 virtual nodes: distribution imbalance 1.2x, remapped 24% (≈ theoretical 25%)

More virtual nodes → better distribution, more memory (negligible in practice)
Production default: 100-200 virtual nodes per physical node
```

**Consistent hashing vs modulo — decision:**
```
Modulo routing:       perfect distribution, terrible on topology change
Consistent hashing:   near-uniform distribution, minimal redistribution
Use consistent hashing whenever shard topology can change
```

---

## Lab 2.6 — Rebalancing & Resharding

**The problem:** Adding a shard to a live system requires moving data without downtime or data loss.

**Migration state machine:**
```
STABLE     → normal routing, single ring
MIGRATING  → dual ring, double-write active, backfill running
COMPLETE   → new ring fully active, old shard data can be cleaned up
```

**Dual ring routing rules:**
```
MIGRATING read  → stableRing  (data guaranteed to be there)
MIGRATING write → hashRing    (new topology)
                + stableRing  (if key moved, double-write to old location too)

Only ~25% of keys need double-write (consistent hashing guarantee)
75% of keys didn't move — write once to same shard as before
```

**Backfill pattern:**
```java
// Batch by range — never load all IDs at once
int lastId = 0;
while (true) {
    List<Integer> batch = shard.query(
        "SELECT DISTINCT user_id FROM orders WHERE user_id > ? ORDER BY user_id LIMIT 1000",
        lastId);
    if (batch.isEmpty()) break;
    lastId = batch.getLast();  // advance by full batch, not filtered subset

    batch.stream()
        .filter(id -> newRing.getNode(id) != stableRing.getNode(id))
        .forEach(id -> copyToNewShard(id, sourceShard));
}
```

**Backfill must be idempotent:**
```sql
-- Check before insert — running backfill twice must be safe
SELECT count(*) FROM orders WHERE user_id = ? AND created_at = ?
-- Only insert if count = 0
```

**Production-grade backfill uses CDC (Debezium):**
```
Table scan backfill   → works, but scans all rows to find impacted ones
CDC with Debezium     → streams WAL events, routes each to correct shard
                      → initial snapshot + streaming = no gap, no missed writes
                      → covered in Lab 3.4b after Kafka is introduced
```

**Migration state storage:**
```
Env variable  → simple, requires restart to change (acceptable for planned migrations)
Redis flag    → instant change, all instances pick up simultaneously
Feature flag  → audit trail, per-instance rollout
```

**Replication slot warning (PostgreSQL + Debezium):**
```
DROP the Debezium replication slot immediately after migration completes.
An inactive slot prevents WAL cleanup → disk fills up → database stops.
This is one of the most dangerous operational mistakes in PostgreSQL.
```

---

## Lab 2.7 — Zero-Downtime Schema Migrations

**Skipped** — concepts already internalized. Key rules for reference:

```
ALTER TABLE on large tables → acquires ACCESS EXCLUSIVE lock
                            → blocks all reads and writes
                            → duration scales with row count
                            → dangerous above ~10M rows

Safe patterns:
  ADD COLUMN nullable        → instant (catalog only, no row rewrite)
  ADD COLUMN with DEFAULT    → instant in PostgreSQL 11+ (catalog only)
  CREATE INDEX CONCURRENTLY  → builds without locking, takes longer
  NOT VALID constraint       → adds FK without full validation scan
  Expand-and-contract        → universal pattern for any breaking change

Expand-and-contract for column rename:
  1. Add new column (nullable)
  2. Dual-write both columns in application
  3. Backfill old column → new column in batches
  4. Switch reads to new column
  5. Stop writing to old column
  6. Drop old column
  Each step is a separate deployment. Never in one migration.
```

**Schema migration review threshold:** Any migration on a table above 10M rows requires review of lock duration and rollback plan before running in production.

---

## Cross-cutting patterns

**DataSource autoconfiguration rule (memorize this):**
```
Manual @Bean DataSource → Spring autoconfiguration backs off
                        → spring.datasource.hikari.* ignored
                        → must configure HikariCP explicitly in code
```

**Spring profiles for multi-environment:**
```
application.properties          → always loaded (shared config)
application-local.properties    → SPRING_PROFILES_ACTIVE=local
application-cloud.properties    → SPRING_PROFILES_ACTIVE=cloud
application-shard.properties    → SPRING_PROFILES_ACTIVE=shard

Profile-specific overrides base. @Profile("x") on @Configuration/@Component
controls which beans load per environment.
```

**The scaling ladder — exhaust in order:**
```
1. Query optimization + indexes          (free, always first)
2. Read replicas                         (scales reads, not writes)
3. Table partitioning                    (scales large tables, same DB)
4. Vertical scaling                      (bigger machine, has limits)
5. Connection pooling (PgBouncer)        (protects DB from connection storms)
6. Sharding                              (scales writes, last resort)
```
