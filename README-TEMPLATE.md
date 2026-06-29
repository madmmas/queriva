# Queriva

<!-- CI badge — replace with actual workflow badge after CI is green -->
[![CI](https://github.com/madmmas/queriva/actions/workflows/ci.yml/badge.svg)](https://github.com/madmmas/queriva/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-teal.svg)](LICENSE)
[![Version](https://img.shields.io/github/v/tag/madmmas/queriva?label=version)](https://github.com/madmmas/queriva/releases)

**Ask your data anything.**

Queriva is a self-hosted, LLM-powered semantic search engine. Ingest documents
from any source, search them in natural language, and get AI-synthesized answers
— with zero runtime calls to external APIs. Everything runs locally via Docker.

---

## What it does

- **Ingest** — push documents from JSON, CSV, files, or URLs into a local Qdrant vector store
- **Search** — query in plain language and get ranked results by semantic similarity
- **Answer** — RAG mode synthesizes answers using a local LLM (Ollama + Mistral 7B)
- **Embed** — consume as a micro-frontend widget in any React host app

Multilingual by default — tested with Bangla and English via LaBSE.

---

## Quick start

> Requires: Docker, Docker Compose, 8GB RAM (for Mistral 7B)

```bash
# 1. Clone
git clone https://github.com/madmmas/queriva.git
cd queriva

# 2. Configure
cp .env.example .env

# 3. Start all services (first run downloads Mistral ~4GB — be patient)
docker compose up -d

# 4. Seed demo data
make seed

# 5. Open the UI
open http://localhost:3000
```

Search for `"floods in Dhaka last week"` to verify everything is working.

---

## Architecture

```
INGESTION PATH                     SEARCH PATH
──────────────                     ───────────
Files / JSON / CSV / URLs          Standalone UI  +  MFE Widget
        │                                   │
        ▼                                   ▼
  Ingest API (Spring Boot) ◄─────── Search API (Spring Boot)
        │                                   │
        ▼                                   ├──► Ollama (RAG synthesis)
  Embed Sidecar (FastAPI)                   │
        │                                   │
        ▼                                   ▼
              Qdrant (vector store)
```

Full architecture: [`docs/SPEC.md §4`](docs/SPEC.md)

---

## Ingestion

### Via API

```bash
# Create a collection
curl -X POST http://localhost:8080/api/ingest/collection \
  -H "Content-Type: application/json" \
  -d '{"collection":"my_docs","vector_size":768,"distance":"Cosine"}'

# Ingest documents
curl -X POST http://localhost:8080/api/ingest/documents \
  -H "Content-Type: application/json" \
  -d '{
    "collection": "my_docs",
    "documents": [{
      "id": "doc-001",
      "title": "My document title",
      "body": "Full text of the document...",
      "source": "internal",
      "language": "en"
    }]
  }'
```

### Via CLI

```bash
# JSON file
python packages/ingest-cli/queriva_ingest.py \
  --api http://localhost:8080 \
  --collection my_docs \
  --format json \
  --source ./my_documents.json

# CSV with column mapping
python packages/ingest-cli/queriva_ingest.py \
  --api http://localhost:8080 \
  --collection my_docs \
  --format csv \
  --source ./articles.csv \
  --map title=headline body=content published_at=date

# Directory of files (.txt, .md, .pdf)
python packages/ingest-cli/queriva_ingest.py \
  --api http://localhost:8080 \
  --collection my_docs \
  --format files \
  --source ./documents/
```

Full ingestion guide: [`docs/SPEC.md §7`](docs/SPEC.md)

---

## Search

### Via API

```bash
# Search mode — ranked results only
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query":"floods in Dhaka","collection":"news_radar","mode":"search"}'

# RAG mode — AI-synthesized answer
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query":"floods in Dhaka","collection":"news_radar","mode":"rag"}'
```

### Via UI

Open `http://localhost:3000`, type your query, toggle between Search and RAG mode.

---

## Embed as a widget

```typescript
// vite.config.ts (host app)
federation({
  remotes: {
    queriva: 'http://localhost:5173/assets/remoteEntry.js'
  }
})

// In your component
import SearchWidget from 'queriva/SearchWidget'

<SearchWidget
  apiUrl="http://localhost:8080"
  collection="my_docs"
  defaultMode="rag"
/>
```

Full MFE guide: [`docs/SPEC.md §11`](docs/SPEC.md)

---

## News Radar integration

```python
import httpx

def push_to_queriva(article: dict):
    httpx.post("http://queriva-api:8080/api/ingest/documents", json={
        "collection": "news_radar",
        "model": "LaBSE",
        "documents": [{
            "id": article["cluster_id"],
            "title": article["title"],
            "body": article["body"],
            "source": article["source"],
            "language": article["language"],
            "published_at": article["published_at"],
            "url": article["url"]
        }],
        "upsert_mode": "skip_existing"
    })
```

---

## Configuration

Copy `.env.example` to `.env` and adjust as needed.

Key variables:

| Variable | Default | Description |
|---|---|---|
| `OLLAMA_MODEL` | `mistral` | Local LLM model |
| `EMBED_DEFAULT_MODEL` | `LaBSE` | Embedding model |
| `SEARCH_MIN_SCORE` | `0.60` | Minimum cosine similarity |
| `SEARCH_MAX_SCORE_AUTO_ACCEPT` | `0.80` | Skip LLM if score exceeds this |
| `INGEST_DEFAULT_CHUNK_SIZE` | `512` | Characters per chunk |

Full config reference: [`docs/SPEC.md §13`](docs/SPEC.md)

---

## Development

```bash
# Install all dependencies
make install

# Run tests (unit + integration)
make test

# Unit tests only (no Docker)
make test-unit

# Integration tests (Docker required)
make test-int

# E2E smoke test
make smoke

# Run individual packages
make test-api
make test-embed
make test-ingest
make test-ui
```

---

## Packages

| Package | Stack | Role |
|---|---|---|
| `packages/api` | Spring Boot 3, Java 21 | Search + ingest gateway |
| `packages/embed-sidecar` | FastAPI, Python 3.11 | Embedding generation |
| `packages/ingest-cli` | Python 3.11 CLI | Batch ingestion |
| `packages/ui` | React 18, Vite, TypeScript | Standalone app + MFE widget |

---

## Troubleshooting

**Model download is slow on first start**
Mistral 7B is ~4GB. First `docker compose up` will take several minutes.
Monitor with: `docker compose logs -f ollama`

**Embed sidecar is slow on first query**
LaBSE (~1.8GB) loads on first request. Subsequent requests are fast.

**`GET /api/health` shows embed_sidecar: disconnected**
Ensure embed-sidecar is running: `docker compose up embed-sidecar`
Check logs: `docker compose logs embed-sidecar`

**Search returns no results**
Verify the collection was seeded: `GET http://localhost:8080/api/ingest/collections`
Ensure the embedding model matches the one used at ingest time.

**Model mismatch error**
The model used at search time must match the model used at ingest time.
If you ingested with LaBSE, search must also use LaBSE.

---

## Docs

- [`docs/SPEC.md`](docs/SPEC.md) — full project specification
- [`docs/ISSUES.md`](docs/ISSUES.md) — implementation backlog
- [`docs/adr/`](docs/adr/) — architecture decision records
- [`CHANGELOG.md`](CHANGELOG.md) — version history

---

## License

MIT — see [LICENSE](LICENSE)

---

## Naming

**Queriva** *(kweh-REE-va)* — Query + Retrieve, with Riva (riverbank): where your query lands.
