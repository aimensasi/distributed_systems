# Module 0.5 — Java Concurrency Reference Sheet

---

## The Three Thread Safety Problems

| Problem | Cause | Fix | When |
|---|---|---|---|
| Visibility | CPU cache — reader sees stale value | `volatile` | Single writer, many readers, simple flag |
| Atomicity (Read-Modify-Write) | `count++` is 3 ops — thread interrupts between them | `AtomicInteger` / `synchronized` | Counter, any compound operation |
| Atomicity (Check-Then-Act) | Gap between check and act | `compareAndSet` | Conditional updates, lazy init |

These are independent. Fixing one does not fix the others.

---

## CAS Loop — The Pattern

```java
while (true) {
    T current = ref.get();
    T next = compute(current);
    if (ref.compareAndSet(current, next)) return;
    // lost the race — retry with fresh read
}
```

**Why it's safe:** every retry means another thread succeeded. The system is making progress.

---

## Thread Pool Decision Tree

```
Task arrives
  ├── core thread free?       → assign immediately
  ├── queue not full?         → enqueue, wait for free thread
  ├── below maxPoolSize?      → create extra thread
  └── everything full?        → rejection policy fires
```

**The silent misconfiguration:** unbounded queue + high `maxPoolSize` = extra threads never created, `maxPoolSize` is meaningless, queue grows until OOM.

**CallerRunsPolicy** = free back-pressure. Caller runs the task itself, naturally slows the producer down.

---

## BlockingQueue — The Methods That Matter

```java
put()    // blocks when full  — always use in producers, never add()
take()   // blocks when empty — always use in consumers, never poll()
```

**Poison pill shutdown:**
```java
// producer signals done
queue.put(POISON_PILL);

// consumer exits and passes pill along
if (item == POISON_PILL) {
    queue.put(POISON_PILL); // for the next consumer
    break;
}
```

---

## CountDownLatch vs CyclicBarrier

| | CountDownLatch | CyclicBarrier |
|---|---|---|
| Who waits | Observer (e.g. main thread) | The workers themselves |
| Pattern | "Notify me when all N are done" | "Nobody moves until we all arrive" |
| Reusable | No | Yes |

---

## Deadlock — Two Conditions Required

1. Holding one lock while waiting for another
2. Opposite acquisition order between threads

**Fix 1:** Consistent lock ordering — both threads acquire in the same order.
**Fix 2:** `tryLock()` with timeout — back off and retry if you can't get both locks.

---

## The Distributed Mapping

| Java | Distributed Equivalent | Lab |
|---|---|---|
| `volatile` | Replication lag | 2.1 |
| CAS / optimistic lock | Version columns in PostgreSQL | 1.1 |
| `synchronized` | Redis distributed lock | 1.2 |
| `Semaphore` | `Redis::throttle()` | 1.4 |
| `BlockingQueue` | Kafka back-pressure | 3.3 |
| `CallerRunsPolicy` | Producer self-throttling | 3.3 |
| `ThreadPoolExecutor` | Tomcat thread pool sizing | Every lab |

---