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

echo "=== Queriva demo seed ==="
echo "API:        $API_URL"
echo "Collection: $COLLECTION"
echo "Model:      $MODEL"
echo "Fixture:    $FIXTURE"
echo ""

# Check fixture exists
if [ ! -f "$FIXTURE" ]; then
  echo "ERROR: fixture file not found: $FIXTURE"
  echo "Run from repo root: ./scripts/seed-demo.sh"
  exit 1
fi

# Check API is reachable
echo "1. Checking API health..."
HEALTH=$(curl -sf "$API_URL/api/health" || echo "UNREACHABLE")
if echo "$HEALTH" | grep -q '"qdrant":"connected"'; then
  echo "   API healthy. Qdrant connected."
else
  echo "ERROR: API not reachable or Qdrant not connected at $API_URL"
  echo "Run: docker compose up -d && sleep 10"
  exit 1
fi

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
curl -sf "$API_URL/api/ingest/collections" | grep -o "\"$COLLECTION\"" > /dev/null \
  && echo "   Collection '$COLLECTION' confirmed."

echo ""
echo "=== Seed complete ==="
echo "   Ingested: $INGESTED documents"
echo "   Skipped:  $SKIPPED (already existed)"
echo "   Chunks:   $CHUNKS Qdrant points"
echo ""
echo "Try a search:"
echo "  curl -X POST $API_URL/api/search \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"query\":\"floods in Dhaka last week\",\"collection\":\"$COLLECTION\",\"mode\":\"search\"}'"
