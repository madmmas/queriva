#!/usr/bin/env bash
# Pulls the default Ollama model into the docker compose ollama service (ADR-006, issue #16).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

OLLAMA_MODEL="${OLLAMA_MODEL:-mistral}"
OLLAMA_HOST="${OLLAMA_HOST:-http://localhost:11434}"
OLLAMA_HEALTH_PATH="/api/tags"
MAX_WAIT_SECONDS=120
WAIT_INTERVAL_SECONDS=2

cd "${REPO_ROOT}"

echo "=== Queriva Ollama model pull ==="
echo "Model: ${OLLAMA_MODEL}"
echo "Host:  ${OLLAMA_HOST}"

wait_for_ollama() {
  local elapsed=0
  while (( elapsed < MAX_WAIT_SECONDS )); do
    if curl -sf "${OLLAMA_HOST}${OLLAMA_HEALTH_PATH}" >/dev/null; then
      return 0
    fi
    sleep "${WAIT_INTERVAL_SECONDS}"
    elapsed=$((elapsed + WAIT_INTERVAL_SECONDS))
  done
  return 1
}

if ! wait_for_ollama; then
  echo "ERROR: Ollama is not reachable at ${OLLAMA_HOST} after ${MAX_WAIT_SECONDS}s." >&2
  echo "Start it with: docker compose up -d ollama" >&2
  exit 1
fi

if curl -sf "${OLLAMA_HOST}${OLLAMA_HEALTH_PATH}" | grep -q "\"name\":\"${OLLAMA_MODEL}\""; then
  echo "Model '${OLLAMA_MODEL}' is already available — skipping pull."
  exit 0
fi

echo "Pulling '${OLLAMA_MODEL}' (first run downloads ~4GB; this may take several minutes)..."
docker compose exec -T ollama ollama pull "${OLLAMA_MODEL}"
echo "=== Ollama model pull complete ==="
