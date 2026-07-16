# Queriva — Project Specification

> **"Ask your data anything."**
> Natural language search over your own data. Fully local. No external APIs.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Goals](#2-goals)
3. [Non-Goals (v1)](#3-non-goals-v1)
4. [Architecture](#4-architecture)
5. [Repo Structure](#5-repo-structure)
6. [API Contract](#6-api-contract)
7. [Data Ingestion](#7-data-ingestion)
8. [Search Flow](#8-search-flow-detailed)
9. [Embedding Sidecar](#9-embedding-sidecar)
10. [LLM Synthesis Prompt](#10-llm-synthesis-prompt)
11. [Micro-frontend (MFE) Design](#11-micro-frontend-mfe-design)
12. [Docker Compose](#12-docker-compose)
13. [Configuration](#13-configuration-envexample)
14. [Qdrant Payload Schema](#14-qdrant-payload-schema)
15. [Search Modes](#15-search-modes)
16. [Tech Stack Summary](#16-tech-stack-summary)
17. [Milestones](#17-milestones)
18. [Version Release Guide](#18-version-release-guide)
19. [ADR Index](#19-adr-index)
20. [Naming](#20-naming)

---

## 1. Overview

Queriva is a self-hosted, LLM-powered semantic search engine. It accepts natural
language queries, embeds them using a local embedding model, performs vector
similarity search against a Qdrant collection, and synthesizes answers using a
locally running LLM via Ollama. It has zero runtime dependencies on external
services.

It is designed to be:

- **Standalone** — runs entirely via Docker Compose
- **Embeddable** — ships a micro-frontend widget consumable by any host app (AIPlane, News Radar, etc.)
- **Data-agnostic** — works with any Qdrant collection, any embedding model, any domain
- **Open-source** — MIT licensed, engineer-first DX

---

## 2. Goals

| Goal | Description |
|---|---|
| Local-first | Zero calls to OpenAI, Anthropic, or any external API at runtime |
| Pluggable data | Consumer brings their own Qdrant collection and embedding model |
| Dual UI modes | Standalone web app + importable micro-frontend widget |
| Multilingual | Supports multilingual embedding models (e.g. LaBSE for Bangla/English) |
| RAG + pure search | Toggle between ranked results only vs. LLM-synthesized answer |
| Engineer DX | One `docker compose up` to run everything |

---

## 3. Non-Goals (v1)

- No authentication / multi-tenancy (v1 is single-user, local)
- No cloud deployment mode
- No streaming ingestion from external message brokers in v1 (Kafka/Redpanda planned for v2)
- No support for non-Qdrant vector stores in v1

---

## 4. Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                             Queriva                                   │
│                                                                       │
│  INGESTION PATH                      SEARCH PATH                     │
│  ─────────────────────               ──────────────────────────────  │
│                                                                       │
│  Raw documents (files,               ┌────────────────┐              │
│  JSON, API, CSV)                     │  Standalone UI │              │
│         │                            │  (React SPA)   │              │
│         ▼                            └───────┬────────┘              │
│  ┌─────────────────┐                         │                       │
│  │  Ingest API     │  POST /api/ingest        │  ┌────────────────┐  │
│  │  (Spring Boot)  │                          │  │  MFE Widget    │  │
│  └────────┬────────┘                          │  │  (Module Fed.) │  │
│           │                                   │  └───────┬────────┘  │
│           ▼                                   └──────────┘           │
│  ┌─────────────────┐                                │                │
│  │  Embed Sidecar  │◄───────────────────────────────┤                │
│  │  (FastAPI)      │  embed query / embed doc        │                │
│  └────────┬────────┘                                │                │
│           │                              ┌──────────▼──────────┐     │
│           │ upsert vectors               │   Search API        │     │
│           ▼                              │   (Spring Boot)     │     │
│  ┌─────────────────┐                    └──────────┬──────────┘     │
│  │     Qdrant      │◄──────────── vector search ───┘                │
│  │   (Vector DB)   │                                                 │
│  └─────────────────┘                    ┌──────────────────┐        │
│                                         │  Ollama (LLM)    │        │
│                                         │  synthesis only  │        │
│                                         └──────────────────┘        │
└──────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Tech | Responsibility |
|---|---|---|
| `packages/ui` | React + Vite + Module Federation | Standalone SPA + embeddable widget |
| `packages/api` | Spring Boot (Java 21) | Search gateway + ingestion orchestrator |
| `packages/embed-sidecar` | FastAPI (Python) | Embedding generation via sentence-transformers |
| `packages/ingest-cli` | Python CLI | Batch ingestion tool for files, JSON, CSV, URLs |
| Qdrant | Docker (qdrant/qdrant) | Vector storage and ANN search |
| Ollama | Docker (ollama/ollama) | Local LLM inference (Mistral, Llama3, Phi-3) |

---

## 5. Repo Structure

```
queriva/
├── packages/
│   ├── api/                      ← Search + ingest gateway (Spring Boot)
│   │   ├── src/main/java/
│   │   │   └── dev/queriva/
│   │   │       ├── search/
│   │   │       │   ├── SearchController.java
│   │   │       │   ├── QueryEmbeddingService.java
│   │   │       │   ├── QdrantSearchService.java
│   │   │       │   ├── LLMSynthesisService.java
│   │   │       │   └── SearchResultMapper.java
│   │   │       └── ingest/
│   │   │           ├── IngestController.java
│   │   │           ├── IngestService.java
│   │   │           ├── ChunkingService.java
│   │   │           └── CollectionManager.java
│   │   └── pom.xml
│   │
│   ├── embed-sidecar/            ← Embedding FastAPI service
│   │   ├── main.py
│   │   ├── models.py
│   │   └── requirements.txt
│   │
│   ├── ingest-cli/               ← Batch ingestion CLI (Python)
│   │   ├── queriva_ingest.py     ← CLI entry point
│   │   ├── loaders/
│   │   │   ├── json_loader.py
│   │   │   ├── csv_loader.py
│   │   │   ├── file_loader.py    ← txt, md, pdf
│   │   │   └── url_loader.py     ← scrape + ingest URLs
│   │   ├── chunker.py
│   │   ├── requirements.txt
│   │   └── README.md
│   │
│   └── ui/                       ← React micro-frontend
│       ├── src/
│       │   ├── App.tsx            ← Standalone entry
│       │   ├── SearchWidget.tsx   ← Embeddable widget (MFE export)
│       │   ├── components/
│       │   │   ├── SearchBar.tsx
│       │   │   ├── ResultCard.tsx
│       │   │   ├── AISummary.tsx
│       │   │   └── ModeToggle.tsx
│       │   └── hooks/
│       │       └── useSearch.ts
│       ├── vite.config.ts         ← Module Federation config
│       └── package.json
│
├── docs/
│   ├── SPEC.md                   ← this file
│   ├── ISSUES.md                 ← implementation backlog
│   └── adr/                      ← Architecture Decision Records
│       ├── ADR-001-qdrant.md
│       ├── ADR-002-labse.md
│       ├── ADR-003-chunking-strategy.md
│       ├── ADR-004-spring-boot.md
│       ├── ADR-005-embed-sidecar.md
│       ├── ADR-006-ollama-mistral.md
│       ├── ADR-007-char-chunking.md
│       ├── ADR-008-document-id.md
│       ├── ADR-009-testcontainers.md
│       ├── ADR-010-module-federation.md
│       ├── ADR-011-ingest-in-api.md
│       └── ADR-012-turborepo.md
│
├── .cursor/
│   └── rules/
│       └── queriva.mdc           ← Cursor AI workflow rules
│
├── fixtures/
│   └── news_radar_dhaka_floods.json
│
├── scripts/
│   └── seed-demo.sh
│
├── CHANGELOG.md
├── docker-compose.yml
├── docker-compose.dev.yml
├── .env.example
├── Makefile
└── README.md
```

---

## 6. API Contract

### `POST /api/search`

**Request**
```json
{
  "query": "floods in Dhaka last week",
  "collection": "news_radar",
  "top_k": 10,
  "min_score": 0.40,
  "mode": "rag",
  "filters": {
    "language": "bn",
    "date_from": "2026-06-01",
    "date_to": "2026-06-28",
    "category": "national"
  }
}
```

**Fields**

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `query` | string | yes | — | Natural language query |
| `collection` | string | yes | — | Qdrant collection name |
| `top_k` | int | no | 10 | Number of results to retrieve |
| `min_score` | float | no | 0.40 | Minimum cosine similarity threshold (LaBSE-tuned; raise for stricter filtering) |
| `mode` | enum | no | `search` | `search` (ranked results only) or `rag` (LLM synthesis) |
| `filters` | object | no | null | Qdrant payload filters |

**Response**
```json
{
  "query": "floods in Dhaka last week",
  "mode": "rag",
  "summary": "Three major floods hit Dhaka between June 14–20...",
  "results": [
    {
      "id": "uuid",
      "score": 0.892,
      "title": "Buriganga river floods Dhaka low-lying areas",
      "snippet": "Heavy monsoon rains caused the Buriganga river to overflow...",
      "source": "prothomalo.com",
      "language": "bn",
      "published_at": "2026-06-15T08:30:00Z",
      "url": "https://..."
    }
  ],
  "latency_ms": {
    "embed": 45,
    "search": 23,
    "synthesis": 1840,
    "total": 1908
  }
}
```

### `POST /api/embed` *(embed-sidecar)*

```json
{ "text": "floods in Dhaka last week", "model": "LaBSE" }
→ { "vector": [0.12, 0.34, ...], "dimensions": 768 }
```

### `POST /api/ingest/documents`

```json
{
  "collection": "news_radar",
  "model": "LaBSE",
  "documents": [{ "id": "uuid", "title": "...", "body": "...", "source": "...",
                  "language": "bn", "published_at": "2026-06-15T08:30:00Z",
                  "category": "national", "url": "https://..." }],
  "chunking": { "enabled": true, "chunk_size": 512, "overlap": 64 },
  "upsert_mode": "skip_existing"
}
→ { "collection": "news_radar", "ingested": 42, "chunks_created": 187,
    "skipped": 3, "errors": 0, "latency_ms": 4821 }
```

### `POST /api/ingest/collection`

```json
{ "collection": "news_radar", "vector_size": 768, "distance": "Cosine",
  "recreate_if_exists": false }
```

### `GET /api/ingest/collections`

```json
{ "collections": [{ "name": "news_radar", "vector_size": 768,
  "distance": "Cosine", "points_count": 54821 }] }
```

### `DELETE /api/ingest/collection/{name}`

Returns `204 No Content`.

### `GET /api/health`

```json
{ "status": "ok", "qdrant": "connected", "ollama": "connected",
  "embed_sidecar": "connected" }
```

---

## 7. Data Ingestion

### 7.1 Ingestion Flow

```
Raw input (file / JSON / CSV / URL / API push)
        │
        ▼
1. Loader — reads raw content, normalises to Document[]
        │
        ▼
2. Chunker — sliding window with title prepending (ADR-003, ADR-007)
        │
        ▼
3. Embed Sidecar — embeds each chunk via POST /api/embed (batched)
        │
        ▼
4. Qdrant upsert — stores vector + payload per chunk (ADR-008)
        │
        ▼
5. Ingest report — returns ingested, chunks_created, skipped, errors
```

### 7.2 Ingest API Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `collection` | string | yes | — | Target Qdrant collection |
| `model` | string | no | `LaBSE` | Embedding model — must match query-time model (ADR-002) |
| `documents` | array | yes | — | Array of document objects |
| `chunking.enabled` | bool | no | `true` | Split long bodies into chunks |
| `chunking.chunk_size` | int | no | `512` | Max chars per chunk (ADR-007) |
| `chunking.overlap` | int | no | `64` | Overlap chars between chunks |
| `upsert_mode` | enum | no | `skip_existing` | `skip_existing`, `overwrite`, `error_on_conflict` |

### 7.3 Ingest CLI

```bash
# JSON
python queriva_ingest.py --api http://localhost:8080 \
  --collection news_radar --format json --source ./news.json

# CSV with column mapping
python queriva_ingest.py --api http://localhost:8080 \
  --collection news_radar --format csv --source ./articles.csv \
  --map title=headline body=content source=outlet published_at=date

# Directory of files
python queriva_ingest.py --api http://localhost:8080 \
  --collection my_docs --format files --source ./documents/

# URL list
python queriva_ingest.py --api http://localhost:8080 \
  --collection my_web --format urls --source ./urls.txt
```

| Format | Flag | Description |
|---|---|---|
| Directory | `--format files` | Recursively ingests `.txt`, `.md`, `.pdf` |
| JSON | `--format json` | Array of document objects |
| CSV | `--format csv` | Tabular data with column mapping |
| URLs | `--format urls` | One URL per line, fetches + extracts |
| Stdin | `--format jsonl` | Newline-delimited JSON stream |

### 7.4 Chunking Strategy

See ADR-003 (strategy) and ADR-007 (char vs token).

```
Document (2000 chars)
├── Chunk 1: chars 0–511     ← "Title. body slice..."
├── Chunk 2: chars 448–959   ← 64-char overlap
├── Chunk 3: chars 896–1407
└── Chunk 4: chars 1344–1855
```

- Title prepended to every chunk before embedding
- Each chunk stores `document_id` linking back to source doc
- `body_snippet` in Qdrant payload = first 500 chars of chunk (no title prefix)

### 7.5 Upsert Modes

| Mode | Behaviour |
|---|---|
| `skip_existing` | Skip if point with same `id` exists — safe for incremental ingestion |
| `overwrite` | Replace existing point (vector + payload) |
| `error_on_conflict` | Error if any `id` already exists |

### 7.6 News Radar → Queriva Integration

```python
# In News Radar pipeline (post-dedup step)
import httpx

def push_to_queriva(article: dict):
    httpx.post("http://queriva-api:8080/api/ingest/documents", json={
        "collection": "news_radar",
        "model": "LaBSE",
        "documents": [{
            "id": article["cluster_id"],   # dedup key from News Radar (ADR-008)
            "title": article["title"],
            "body": article["body"],
            "source": article["source"],
            "language": article["language"],
            "published_at": article["published_at"],
            "category": article["category"],
            "url": article["url"]
        }],
        "upsert_mode": "skip_existing"
    })
```

No changes to News Radar's core pipeline — Queriva is an append-only sink.

---

## 8. Search Flow (Detailed)

```
1. User query arrives at POST /api/search
        │
2. QueryEmbeddingService → POST embed-sidecar /api/embed
   Returns 768-dim vector (LaBSE or configured model)
        │
3. QdrantSearchService → Qdrant gRPC/REST
   - search(collection, vector, top_k, filters)
   - filter results below min_score threshold
        │
4a. mode=search → return ranked results directly
4b. mode=rag    → LLMSynthesisService → Ollama /api/generate
    - Build prompt: system + numbered articles + user query (§10)
    - Optionally skip LLM if top score ≥ SEARCH_MAX_SCORE_AUTO_ACCEPT
        │
5. SearchResultMapper → format + return JSON response
```

---

## 9. Embedding Sidecar

See ADR-005 for why this is a separate FastAPI process rather than ONNX-in-JVM.

```python
# main.py (simplified)
from fastapi import FastAPI
from sentence_transformers import SentenceTransformer

app = FastAPI()
models = {}

@app.post("/api/embed")
async def embed(req: EmbedRequest):
    if req.model not in models:
        models[req.model] = SentenceTransformer(req.model)
    vector = models[req.model].encode(req.text).tolist()
    return {"vector": vector, "dimensions": len(vector)}
```

**Supported models** (see ADR-002 for selection rationale):

| Model | Dimensions | Use case |
|---|---|---|
| `LaBSE` | 768 | Default — multilingual (Bangla/English) |
| `all-MiniLM-L6-v2` | 384 | English only, fast |
| `paraphrase-multilingual-mpnet-base-v2` | 768 | Multilingual alternative |

**Critical:** Query embedding model must match document embedding model.
The collection metadata stores the model name used at ingest time.

---

## 10. LLM Synthesis Prompt

```
System:
You are a search assistant. Answer the user's question using ONLY
the provided articles. Cite article titles inline. Be concise.
If the articles don't contain the answer, say so clearly.

Articles:
[1] Title: "Buriganga river floods Dhaka..."
    Source: prothomalo.com | Date: 2026-06-15
    Text: Heavy monsoon rains caused...

[2] Title: "200,000 affected by Dhaka flooding"
    Source: thedailystar.net | Date: 2026-06-17
    Text: ...

User question: floods in Dhaka last week
```

See ADR-006 for local LLM selection (Ollama + Mistral 7B).

---

## 11. Micro-frontend (MFE) Design

See ADR-010 for why Module Federation over iframe or Web Components.

### Widget Props

```typescript
interface SearchWidgetProps {
  apiUrl: string;           // Queriva API base URL
  collection: string;       // Qdrant collection name
  placeholder?: string;     // Search bar placeholder text
  defaultMode?: 'search' | 'rag';
  theme?: 'light' | 'dark' | 'auto';
  filters?: {
    language?: string;
    category?: string;
  };
  onResultClick?: (result: SearchResult) => void;
}
```

### Consumption in AIPlane / News Radar

```typescript
// vite.config.ts (host app)
federation({
  remotes: {
    queriva: 'http://localhost:5173/assets/remoteEntry.js'
  }
})

// In host component
import SearchWidget from 'queriva/SearchWidget'

<SearchWidget
  apiUrl="http://localhost:8080"
  collection="news_radar"
  defaultMode="rag"
  filters={{ language: 'bn' }}
/>
```

---

## 12. Docker Compose

```yaml
version: '3.9'
services:

  qdrant:
    image: qdrant/qdrant:latest
    ports: ["6333:6333", "6334:6334"]
    volumes: ["qdrant_data:/qdrant/storage"]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:6333/healthz"]
      interval: 10s
      retries: 5

  ollama:
    image: ollama/ollama:latest
    ports: ["11434:11434"]
    volumes: ["ollama_data:/root/.ollama"]
    environment:
      - OLLAMA_MODELS=mistral

  embed-sidecar:
    build: ./packages/embed-sidecar
    ports: ["8001:8001"]
    environment:
      - DEFAULT_MODEL=LaBSE
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8001/api/health"]
      interval: 10s
      retries: 5

  api:
    build: ./packages/api
    ports: ["8080:8080"]
    environment:
      - QDRANT_URL=http://qdrant:6333
      - OLLAMA_URL=http://ollama:11434
      - EMBED_SIDECAR_URL=http://embed-sidecar:8001
    depends_on:
      qdrant: { condition: service_healthy }
      embed-sidecar: { condition: service_healthy }

  ui:
    build: ./packages/ui
    ports: ["3000:3000"]
    environment:
      - VITE_API_URL=http://localhost:8080
    depends_on: [api]

volumes:
  qdrant_data:
  ollama_data:
```

---

## 13. Configuration (`.env.example`)

```env
# Ingestion
INGEST_DEFAULT_CHUNK_SIZE=512
INGEST_DEFAULT_OVERLAP=64
INGEST_DEFAULT_UPSERT_MODE=skip_existing
INGEST_BATCH_SIZE=32

# Qdrant
QDRANT_URL=http://localhost:6333
QDRANT_API_KEY=                         # leave empty for local

# Ollama
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=mistral                    # or llama3, phi3, gemma

# Embed Sidecar
EMBED_SIDECAR_URL=http://localhost:8001
EMBED_DEFAULT_MODEL=LaBSE

# API
API_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Search
SEARCH_DEFAULT_TOP_K=10
SEARCH_MIN_SCORE=0.40                   # LaBSE cosine on news corpora; raise toward 0.60 for stricter filtering
SEARCH_MAX_SCORE_AUTO_ACCEPT=0.80       # skip LLM for high-confidence hits

# UI
VITE_API_URL=http://localhost:8080
VITE_DEFAULT_COLLECTION=news_radar
VITE_SEARCH_MIN_SCORE=0.40              # must match SEARCH_MIN_SCORE for standalone UI
```

---

## 14. Qdrant Payload Schema

All ingested Qdrant points must include these payload fields.
Search works without them but UI displays richer results with them.

```json
{
  "title": "string",
  "body_snippet": "string (max 500 chars, no title prefix)",
  "source": "string (domain, e.g. prothomalo.com)",
  "language": "string (ISO 639-1: en, bn, fr...)",
  "published_at": "ISO 8601 datetime string",
  "category": "string",
  "url": "string",
  "document_id": "string (required — shared across all chunks of one doc)",
  "cluster_id": "string (optional — dedup cluster ID from upstream pipeline)"
}
```

---

## 15. Search Modes

| Mode | Description | Latency | Use case |
|---|---|---|---|
| `search` | Returns ranked results, no LLM | ~100ms | Fast lookup, browsing |
| `rag` | LLM synthesizes answer from top results | ~2–5s | Question answering |

---

## 16. Tech Stack Summary

| Layer | Technology |
|---|---|
| Search + Ingest Gateway | Spring Boot 3.x (Java 21) — see ADR-004 |
| Embedding Sidecar | FastAPI + sentence-transformers — see ADR-005 |
| Ingest CLI | Python 3.11 + httpx + pypdf + trafilatura |
| Vector DB | Qdrant (self-hosted) — see ADR-001 |
| Local LLM | Ollama + Mistral 7B — see ADR-006 |
| Frontend | React 18 + Vite + Module Federation — see ADR-010 |
| Monorepo | Turborepo — see ADR-012 |
| Containerization | Docker Compose |

---

## 17. Milestones

| Phase | Milestone | Issues | Deliverable |
|---|---|---|---|
| 1 | Collection management | #5 | Create, list, delete Qdrant collections |
| 2 | Core ingest API | #7, #8 | chunk → embed → upsert pipeline |
| 3 | Ingest CLI | #9, #10 | JSON, CSV, file, URL, JSONL loaders |
| 4 | Chunking | #6 | Sliding window with title prepending |
| 5 | Core search API | #14, #15 | POST /api/search, ranked results |
| 6 | Embed sidecar | #2, #3 | FastAPI /api/embed, shared by ingest + search |
| 7 | RAG mode | #17, #18 | Ollama synthesis, prompt template |
| 8 | Standalone UI | #19–#24 | React SPA wired to full backend |
| 9 | MFE widget | #25, #26 | Module Federation export |
| 10 | Docker Compose | #27 | One-command full stack |
| 11 | README + Docs | #29 | Setup guide, ingestion guide, architecture |

---

## 18. Version Release Guide

| Version | After issues | What works | Tag criteria |
|---|---|---|---|
| `v0.1.0` | #0–#3 + ADRs | Monorepo + CI + embed sidecar | `turbo run test` exits 0, `/api/embed` returns vectors |
| `v0.2.0` | #4–#11 + ADRs | Full ingest pipeline | Ingest CLI ingests JSON; collections API works |
| `v0.3.0` | #12–#18 | Search + RAG via curl | `/api/search` returns results + RAG summary |
| `v0.4.0` | #19–#24 | Standalone UI | `localhost:3000` search + RAG works in browser |
| `v0.5.0` | #25–#28 | MFE widget + full stack | `make smoke` passes; SearchWidget embeddable |
| `v1.0.0` | #29 | Client-presentable | Fresh clone → working in < 20 min |

**Upwork demo milestones:** v0.3.0 (terminal demo), v0.4.0 (UI demo + blog post), v1.0.0 (proposals).

See `CHANGELOG.md` for per-release entry history.

---

## 19. ADR Index

Architecture Decision Records live in `docs/adr/`. Each captures context,
decision, alternatives considered, and consequences.

| ADR | Title | Before issue | Key decision |
|---|---|---|---|
| [ADR-001](adr/ADR-001-qdrant.md) | Vector DB: Qdrant | #0 | Qdrant over Weaviate, Milvus, pgvector, Chroma |
| [ADR-002](adr/ADR-002-labse.md) | Embedding model: LaBSE | #2 | LaBSE over MiniLM, OpenAI embeddings |
| [ADR-003](adr/ADR-003-chunking-strategy.md) | Chunking: sliding window + title prepend | #6 | Over sentence/paragraph splitting |
| [ADR-004](adr/ADR-004-spring-boot.md) | API language: Spring Boot / Java 21 | #0 | Over Go, FastAPI |
| [ADR-005](adr/ADR-005-embed-sidecar.md) | Embed sidecar: FastAPI process | #2 | Over ONNX Runtime in JVM |
| [ADR-006](adr/ADR-006-ollama-mistral.md) | Local LLM: Ollama + Mistral 7B | #2 | Over vLLM, llama.cpp, LM Studio |
| [ADR-007](adr/ADR-007-char-chunking.md) | Chunking unit: characters | #6 | Over token-based chunking |
| [ADR-008](adr/ADR-008-document-id.md) | Upsert identity: document_id | #7 | Over content hash |
| [ADR-009](adr/ADR-009-testcontainers.md) | Testing: Testcontainers for Qdrant | #1 | Over mock Qdrant client |
| [ADR-010](adr/ADR-010-module-federation.md) | MFE: Module Federation | #25 | Over iframe, Web Components |
| [ADR-011](adr/ADR-011-ingest-in-api.md) | Ingest in Spring Boot | #0 | Over separate Python microservice |
| [ADR-012](adr/ADR-012-turborepo.md) | Monorepo: Turborepo | #0 | Over Nx |

---

## 20. Naming

**Queriva** *(kweh-REE-va)*

- *Query* + *Retrieve* — the two core operations
- *Riva* (Italian/Spanish: riverbank) — where your query lands
- GitHub: `madmmas/queriva`
- License: MIT
