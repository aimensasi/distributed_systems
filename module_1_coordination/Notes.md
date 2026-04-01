## MODULE 1 — COORDINATION & CONSISTENCY
### Reference Sheet

**Lost Updates**
- Read → write is never safe under concurrency without coordination
- Fix: `UPDATE value = value + 1` (atomic), `SELECT FOR UPDATE` (pessimistic), version column (optimistic)
- A transaction alone doesn't fix lost updates at `Read Committed` isolation

**Distributed Locking**
- `SET key token NX PX ttl` — only primitive you need
- Always release with token check — never delete unconditionally
- TTL must be >> max operation duration
- Fencing token (`INCR`) = monotonic counter, DB rejects writes with stale token
- `get` then `delete` is a race — use Lua for atomic check-and-delete

**Idempotency**
- Key comes from client, represents one attempt not one operation
- INSERT first, execute second — claim the key before doing the work
- `23xxx` SQLState = unique constraint violation — this is your duplicate signal
- Status: `pending` → `complete`. Second request while pending = 409
- Failed attempt = new key on retry. Key represents attempt, not intent.

**Rate Limiting**
- Per-instance counter × N instances = broken global limit
- `INCR` + `EXPIRE` as Lua script — non-atomic = race condition
- `EXPIRE` only on count == 1, never reset on subsequent requests

**Leader Election**
- `SETNX` with instanceId as value, TTL as heartbeat
- Check ownership before acquire — don't SETNX if you already hold it
- Renew TTL on every tick, don't release between runs
- TTL expiry = failover window. Short = fast failover, risk of false expiry
- `fixedDelay` not `fixedRate` — next tick only after current finishes

**Isolation Levels (quick)**
- `Read Committed` (PG default) — dirty reads prevented, lost updates possible
- `Repeatable Read` — PG prevents lost updates, MySQL does not
- `Serializable` — safe but costly, requires retry on abort

---

Continue to Module 2 in the new chat.