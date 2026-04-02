#!/bin/bash
BASE_URL="http://localhost:8080"

echo "=== Writing orders for users across all shards ==="
# user_id % 3 = 0 → shard 0
curl -s -X POST "$BASE_URL/lab24/orders/3/99.99"
curl -s -X POST "$BASE_URL/lab24/orders/6/49.99"

# user_id % 3 = 1 → shard 1
curl -s -X POST "$BASE_URL/lab24/orders/1/150.00"
curl -s -X POST "$BASE_URL/lab24/orders/4/75.50"

# user_id % 3 = 2 → shard 2
curl -s -X POST "$BASE_URL/lab24/orders/2/200.00"
curl -s -X POST "$BASE_URL/lab24/orders/5/33.00"

echo ""
echo "=== Reading orders per user ==="
echo "User 1 (shard 1):"
curl -s "$BASE_URL/lab24/orders/1"
echo ""

echo "User 2 (shard 2):"
curl -s "$BASE_URL/lab24/orders/2"
echo ""

echo "User 3 (shard 0):"
curl -s "$BASE_URL/lab24/orders/3"
echo ""

echo ""
echo "=== Verifying data landed on correct shards ==="
echo "Shard 0 (port 5441) — should have users 3, 6:"
docker exec lab4_shard0 psql -U lab_user -d lab_db \
  -c "SELECT user_id, amount FROM orders ORDER BY user_id;"

echo "Shard 1 (port 5442) — should have users 1, 4:"
docker exec lab4_shard1 psql -U lab_user -d lab_db \
  -c "SELECT user_id, amount FROM orders ORDER BY user_id;"

echo "Shard 2 (port 5443) — should have users 2, 5:"
docker exec lab4_shard2 psql -U lab_user -d lab_db \
  -c "SELECT user_id, amount FROM orders ORDER BY user_id;"