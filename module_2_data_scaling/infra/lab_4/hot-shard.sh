# Flood user_id=1 with orders (all go to shard 1)
for i in {1..20}; do
  curl -s -X POST "http://localhost:8080/lab24/orders/1/99.99"
done

# Check distribution across shards
docker exec lab4_shard0 psql -U lab_user -d lab_db -c "SELECT count(*) FROM orders;"
docker exec lab4_shard1 psql -U lab_user -d lab_db -c "SELECT count(*) FROM orders;"
docker exec lab4_shard2 psql -U lab_user -d lab_db -c "SELECT count(*) FROM orders;"