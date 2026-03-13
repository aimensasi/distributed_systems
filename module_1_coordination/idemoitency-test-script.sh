KEY=$(uuidgen)

for i in $(seq 1 5); do
  (
    RESP=$(curl -s -H "Idempotency-Key: $KEY" http://localhost:8080/payment)
    printf "[%s] 8080 → %s\n" "$(gdate +%T.%3N)" "$RESP"
  ) &

  (
    RESP=$(curl -s -H "Idempotency-Key: $KEY" http://localhost:8081/payment)
    printf "[%s] 8081 → %s\n" "$(gdate +%T.%3N)" "$RESP"
  ) &
done

wait