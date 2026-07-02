#!/bin/bash
# seed-demo.sh — seed Queriva with the Dhaka floods demo fixture
# Usage: ./scripts/seed-demo.sh [API_URL]
# Default API_URL: http://localhost:8080
# Idempotent: safe to run multiple times (uses skip_existing upsert mode)

set -euo pipefail

API_URL="${1:-http://localhost:8080}"
COLLECTION="news_radar"
MODEL="LaBSE"
FIXTURE="fixtures/news_radar_dhaka_floods.json"
MIN_POINT_COUNT=8
HEALTH_WAIT_MAX_SECONDS=120
HEALTH_WAIT_INTERVAL_SECONDS=2

wait_for_api_health() {
  local elapsed_seconds=0

  while [ "$elapsed_seconds" -lt "$HEALTH_WAIT_MAX_SECONDS" ]; do
    HEALTH=$(curl -sf "$API_URL/api/health" || echo "UNREACHABLE")
    if echo "$HEALTH" | grep -q '"qdrant":"connected"'; then
      echo "   API healthy. Qdrant connected."
      return 0
    fi

    echo "   Waiting for API at $API_URL (${elapsed_seconds}s/${HEALTH_WAIT_MAX_SECONDS}s)..."
    sleep "$HEALTH_WAIT_INTERVAL_SECONDS"
    elapsed_seconds=$((elapsed_seconds + HEALTH_WAIT_INTERVAL_SECONDS))
  done

  echo "ERROR: API not reachable or Qdrant not connected at $API_URL after ${HEALTH_WAIT_MAX_SECONDS}s"
  echo "Run: docker compose up -d --build"
  echo "Last health response: $HEALTH"
  exit 1
}

echo "=== Queriva demo seed ==="
echo "API:        $API_URL"
echo "Collection: $COLLECTION"
echo "Model:      $MODEL"
echo "Fixture:    $FIXTURE"
echo ""

# Validate fixture before touching the API
echo "0. Validating fixture..."
python3 scripts/validate_fixture.py "$FIXTURE"

# Check API is reachable (Spring Boot may still be starting after compose up)
echo "1. Checking API health..."
wait_for_api_health

# Create collection (skip if exists)
echo "2. Creating collection '$COLLECTION' (skip if exists)..."
curl -sf -X POST "$API_URL/api/ingest/collection" \
  -H "Content-Type: application/json" \
  -d "{
    \"collection\": \"$COLLECTION\",
    \"vector_size\": 768,
    \"distance\": \"Cosine\",
    \"recreate_if_exists\": false
  }" > /dev/null && echo "   Collection ready."

# Ingest fixture
echo "3. Ingesting fixture ($FIXTURE)..."
DOCUMENTS=$(cat "$FIXTURE")
RESPONSE=$(curl -sf -X POST "$API_URL/api/ingest/documents" \
  -H "Content-Type: application/json" \
  -d "{
    \"collection\": \"$COLLECTION\",
    \"model\": \"$MODEL\",
    \"documents\": $DOCUMENTS,
    \"chunking\": {
      \"enabled\": true,
      \"chunk_size\": 512,
      \"overlap\": 64
    },
    \"upsert_mode\": \"skip_existing\"
  }")

echo "   Response: $RESPONSE"

INGESTED=$(echo "$RESPONSE" | grep -o '"ingested":[0-9]*' | cut -d: -f2 || echo "?")
SKIPPED=$(echo "$RESPONSE"  | grep -o '"skipped":[0-9]*'  | cut -d: -f2 || echo "?")
CHUNKS=$(echo "$RESPONSE"   | grep -o '"chunks_created":[0-9]*' | cut -d: -f2 || echo "?")

echo ""
echo "4. Verifying collection..."
COLLECTIONS=$(curl -sf "$API_URL/api/ingest/collections")
echo "$COLLECTIONS" | grep -o "\"$COLLECTION\"" > /dev/null \
  && echo "   Collection '$COLLECTION' confirmed."

POINT_COUNT=$(echo "$COLLECTIONS" | python3 -c "
import json, sys
payload = json.load(sys.stdin)
for collection in payload.get('collections', []):
    if collection.get('name') == '$COLLECTION':
        print(collection.get('points_count', 0))
        break
else:
    print(0)
")

if [ -z "$POINT_COUNT" ] || [ "$POINT_COUNT" -lt "$MIN_POINT_COUNT" ]; then
  echo "ERROR: collection '$COLLECTION' has $POINT_COUNT points; expected at least $MIN_POINT_COUNT"
  exit 1
fi
echo "   Point count: $POINT_COUNT (minimum $MIN_POINT_COUNT)"

echo ""
echo "5. Verifying idempotency (second ingest should skip existing documents)..."
SECOND_RESPONSE=$(curl -sf -X POST "$API_URL/api/ingest/documents" \
  -H "Content-Type: application/json" \
  -d "{
    \"collection\": \"$COLLECTION\",
    \"model\": \"$MODEL\",
    \"documents\": $DOCUMENTS,
    \"chunking\": {
      \"enabled\": true,
      \"chunk_size\": 512,
      \"overlap\": 64
    },
    \"upsert_mode\": \"skip_existing\"
  }")

SECOND_INGESTED=$(echo "$SECOND_RESPONSE" | grep -o '"ingested":[0-9]*' | cut -d: -f2 || echo "?")
SECOND_SKIPPED=$(echo "$SECOND_RESPONSE" | grep -o '"skipped":[0-9]*' | cut -d: -f2 || echo "?")
SECOND_CHUNKS=$(echo "$SECOND_RESPONSE" | grep -o '"chunks_created":[0-9]*' | cut -d: -f2 || echo "?")

if [ "$SECOND_INGESTED" != "0" ] || [ "$SECOND_CHUNKS" != "0" ]; then
  echo "ERROR: idempotent re-ingest expected ingested=0 and chunks_created=0"
  echo "       got ingested=$SECOND_INGESTED chunks_created=$SECOND_CHUNKS"
  exit 1
fi

COLLECTIONS_AFTER=$(curl -sf "$API_URL/api/ingest/collections")
POINT_COUNT_AFTER=$(echo "$COLLECTIONS_AFTER" | python3 -c "
import json, sys
payload = json.load(sys.stdin)
for collection in payload.get('collections', []):
    if collection.get('name') == '$COLLECTION':
        print(collection.get('points_count', 0))
        break
else:
    print(0)
")

if [ "$POINT_COUNT_AFTER" != "$POINT_COUNT" ]; then
  echo "ERROR: point count changed after idempotent re-ingest ($POINT_COUNT -> $POINT_COUNT_AFTER)"
  exit 1
fi
echo "   Idempotent re-ingest skipped $SECOND_SKIPPED documents; point count unchanged ($POINT_COUNT_AFTER)."

echo ""
echo "=== Seed complete ==="
echo "   Ingested: $INGESTED documents"
echo "   Skipped:  $SKIPPED (already existed)"
echo "   Chunks:   $CHUNKS Qdrant points"
echo "   Languages: 4 Bangla (bn) + 4 English (en) articles in fixture"
echo ""
echo "Try a search (after issue #14):"
echo "  curl -X POST $API_URL/api/search \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"query\":\"floods in Dhaka last week\",\"collection\":\"$COLLECTION\",\"mode\":\"search\"}'"
