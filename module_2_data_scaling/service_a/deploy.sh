mvn clean package -DskipTests
docker buildx build \
  --platform linux/amd64 \
  -t gcr.io/distributed-systems-490416/lab-2-2 \
  --push \
  .
gcloud run deploy lab-2-2 \
  --image=gcr.io/distributed-systems-490416/lab-2-2 \
  --region=asia-southeast1 \
  --platform=managed \
  --add-cloudsql-instances=distributed-systems-490416:asia-southeast1:lab-db \
  --set-env-vars SPRING_PROFILES_ACTIVE=cloud \
  --memory=512Mi \
  --cpu=1 \
  --max-instances=3 \
  --allow-unauthenticated