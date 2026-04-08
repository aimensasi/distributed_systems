#!/bin/bash
BASE_URL="http://localhost:8080"

echo ""
echo "=== Writing new orders during migration ==="
for userId in 1 2 3 4 5 6 9 12; do
  curl -s -X POST "$BASE_URL/lab24/orders/$userId/150.00" > /dev/null
done
echo "Inserted new orders during migration"

echo ""
echo "=== Data distribution during migration ==="
echo "Shard 0:"
docker exec lab4_shard0 psql -U lab_user -d lab_db \
  -c "SELECT user_id, amount FROM orders ORDER BY user_id;"
echo "Shard 1:"
docker exec lab4_shard1 psql -U lab_user -d lab_db \
  -c "SELECT user_id, amount FROM orders ORDER BY user_id;"
echo "Shard 2:"
docker exec lab4_shard2 psql -U lab_user -d lab_db \
  -c "SELECT user_id, amount FROM orders ORDER BY user_id;"
echo "Shard 3 (should have double-written keys):"
docker exec lab4_shard3 psql -U lab_user -d lab_db \
  -c "SELECT user_id, amount FROM orders ORDER BY user_id;"

echo ""
echo "=== Reading during migration — should come from stable ring ==="
echo "User 1:"
curl -s "$BASE_URL/lab24/orders/1"
echo ""