Problem              Cause                    Solution
─────────────────────────────────────────────────────────────
Read-Modify-Write    count++ is 3 operations  AtomicInteger / synchronized
Check-Then-Act       gap between check & act  compareAndSet / synchronized block
Visibility           CPU cache staleness       volatile