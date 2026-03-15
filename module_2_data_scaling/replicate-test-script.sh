#!/bin/bash

BASE_URL="http://localhost:8080/broken"
KEY="test-key"
VALUE=42

echo "--- Writing value $VALUE to primary ---"
curl -s -X PUT "$BASE_URL/kv/$KEY" \
  -H "Content-Type: application/json" \
  -d "$VALUE"
echo ""

echo "--- Immediately reading from replica ---"
curl -s "$BASE_URL/kv/$KEY"
echo ""

echo ""
echo "--- Waiting 500ms then reading again ---"
sleep 0.5
curl -s "$BASE_URL/kv/$KEY"
echo ""