gcloud logging read \
  "resource.type=cloud_run_revision AND resource.labels.service_name=lab-2-2" \
  --limit=50 \
  --format="value(textPayload)" \
  --project=distributed-systems-490416