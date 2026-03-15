#!/bin/bash

BASE_URL="http://localhost:8080/fixed"
KEY="test-key-4"
VALUE=44

echo "=== Step 1: Write value ==="
curl -s -X PUT "$BASE_URL/kv/$KEY" \
  -H "Content-Type: application/json" \
  -d "$VALUE"
echo ""

echo ""
echo "=== Step 2: Check Redis has WAL entry (proves write side worked) ==="
docker exec -it lab-2-1-redis redis-cli GET "wal:key:$KEY"

echo ""
echo "=== Step 3: Immediate read (replica lagging — should route to primary, return correct value) ==="
curl -s "$BASE_URL/kv/$KEY"
echo ""

echo ""
echo "=== Step 4: Check Redis key still exists (replica hasn't caught up yet) ==="
docker exec -it lab-2-1-redis redis-cli GET "wal:key:$KEY"

echo ""
echo "=== Step 5: Wait for lag + TTL to clear ==="
sleep 6

echo ""
echo "=== Step 6: Check Redis key is gone (TTL expired) ==="
docker exec -it lab-2-1-redis redis-cli GET "wal:key:$KEY"

echo ""
echo "=== Step 7: Read again (should route to replica) ==="
curl -s "$BASE_URL/kv/$KEY"
echo ""