#!/bin/zsh

echo "--- Firing 20 requests (10 per instance) Broken Code ---"

for i in $(seq 1 10); do
  (
    RESP=$(curl -s  https://lab-2-2-554533316554.asia-southeast1.run.app/lab22/slow)
    printf "[%s] 8080 → %s\n" "$(gdate +%T.%3N)" "$RESP"
  ) &

  (
    RESP=$(curl -s http://localhost:8081/api/data-broken)
    printf "[%s] 8081 → %s\n" "$(gdate +%T.%3N)" "$RESP"
  ) &
done

wait