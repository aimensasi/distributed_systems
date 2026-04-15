for i in $(seq 1 10); do
  curl -s localhost:8082/lab31/$i/events/CREATED &
done
wait

for i in $(seq 1 10); do
  curl -s localhost:8082/lab31/$i/events/UPDATED &
done
wait

for i in $(seq 1 10); do
  curl -s localhost:8082/lab31/$i/events/DELETED &
done
wait