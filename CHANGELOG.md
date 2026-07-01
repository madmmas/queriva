# Changelog

All notable changes to Queriva are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions follow [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

- **Added** [api] `ChunkingService` — sliding window 512 chars / 64 overlap with title prepending (#6)
- **Added** [api] `Document` and `Chunk` records for ingest pipeline (#6)
- **Added** [api] `GET /api/health` — live Qdrant, Ollama, and embed-sidecar connectivity (#5)
- **Added** [api] `POST /api/ingest/collection`, `GET /api/ingest/collections`, `DELETE /api/ingest/collection/{name}` (#5)
- **Added** [api] `CollectionManager`, `GlobalExceptionHandler`, CORS config, Qdrant gRPC client (#5)
- **Infrastructure** [api] `Dockerfile` for API package with curl healthcheck (#5)
- **Infrastructure** [infra] `docker-compose.yml` Qdrant service — ports 6333/6334, persistent volume, curl healthcheck (#4)
- **Infrastructure** [infra] `infra/docker/qdrant.Dockerfile` — extends official image with curl for healthchecks (#4)
- **Infrastructure** [api] `QdrantTestcontainersSupport` — shared Testcontainers config aligned with compose (#4)

---

## Release roadmap

| Version | After issues | What it represents |
|---|---|---|
| `v0.1.0` | #0–#3 + ADRs | Foundation: monorepo, CI, embed sidecar |
| `v0.2.0` | #4–#11 + ADRs | Ingest pipeline working end-to-end |
| `v0.3.0` | #12–#18 | Search + RAG via API (no UI) — first Upwork demo |
| `v0.4.0` | #19–#24 | Standalone UI — blog post demo |
| `v0.5.0` | #25–#28 | MFE widget + full docker compose stack |
| `v1.0.0` | #29 | Client-presentable, fully documented |

---

## Entry format

Each entry follows this pattern:

```
- **Category** [package] description (#issue)
```

Categories: `Added`, `Changed`, `Fixed`, `Removed`, `Infrastructure`, `Docs`

Packages: `[api]`, `[embed-sidecar]`, `[ingest-cli]`, `[ui]`, `[infra]`, `[ci]`, `[adr]`, `[docs]`

---

## [0.1.0] — 2026-06-30
> Foundation: monorepo scaffold, CI pipeline, and embed sidecar

### Added
- **Added** [embed-sidecar] `POST /api/embed` — LaBSE, MiniLM, multilingual-mpnet support (#2)
- **Added** [embed-sidecar] `GET /api/health` — sidecar health with `models_loaded` list (#2)
- **Added** [embed-sidecar] Lazy model loading with in-memory cache per model name (#2)
- **Added** [api] Spring Boot 3.4 / Java 21 scaffold with search/ and ingest/ package stubs (#0)
- **Added** [embed-sidecar] FastAPI skeleton with health endpoint stub (#0)
- **Added** [ingest-cli] CLI skeleton with loaders/ directory stubs (#0)
- **Added** [ui] React 18 + Vite + TypeScript scaffold with component stubs (#0)

### Changed
- **Changed** [embed-sidecar] Thread-safe lazy model loading with lock on first load (#3)
- **Changed** [embed-sidecar] Model name pattern validation on POST /api/embed — rejects spaces and invalid chars (#3)
- **Changed** [embed-sidecar] Full test coverage — concurrent load, validation, 90% floor (#3)

### Infrastructure
- **Infrastructure** [infra] Turborepo monorepo with api, embed-sidecar, ingest-cli, ui packages (#0)
- **Infrastructure** [infra] Root Makefile with install, build, test, smoke, seed targets (#0)
- **Infrastructure** [infra] `.env.example` with all SPEC §13 configuration keys (#0)
- **Infrastructure** [infra] MIT LICENSE, root README stub, .gitignore (#0)
- **Infrastructure** [infra] Test infrastructure across all packages — JUnit 5, Testcontainers, WireMock, pytest, Vitest, MSW, jest-axe (#1)
- **Infrastructure** [infra] Test tag conventions: `@Tag("unit")`, `@Tag("integration")`, `@Tag("slow")` documented per package (#1)
- **Infrastructure** [infra] `make test`, `make test-unit`, `make test-int` wired via Turborepo (#1)
- **Infrastructure** [infra] Coverage reporting for embed-sidecar, ingest-cli, and ui (80% floor) (#1)
- **Infrastructure** [ci] GitHub Actions CI — build + test on every PR and push to main (#1b)
- **Infrastructure** [ci] CI caches npm, Maven, and pip dependencies (#1b)
- **Infrastructure** [ci] CI status badge in README (#1b)
- **Infrastructure** [embed-sidecar] Dockerfile with healthcheck on port 8001 (#2)

### Docs
- **Docs** [adr] ADR-001 — Qdrant as vector database
- **Docs** [adr] ADR-002 — LaBSE as default embedding model
- **Docs** [adr] ADR-003 — Sliding window chunking with title prepending
- **Docs** [adr] ADR-004 — Spring Boot / Java 21 for API gateway
- **Docs** [adr] ADR-005 — FastAPI sidecar over ONNX-in-JVM
- **Docs** [adr] ADR-006 — Ollama + Mistral 7B over vLLM / llama.cpp
- **Docs** [adr] ADR-007 — Character-based over token-based chunking
- **Docs** [adr] ADR-008 — Source document_id as upsert identity key
- **Docs** [adr] ADR-009 — Testcontainers over mock Qdrant client
- **Docs** [adr] ADR-010 — Module Federation over iframe / Web Components
- **Docs** [adr] ADR-011 — Ingest orchestration in Spring Boot
- **Docs** [adr] ADR-012 — Turborepo over Nx

---

## [0.2.0] — TBD
> Ingest pipeline: documents flow from any source into Qdrant

### Added
- **Added** [api] `POST /api/ingest/collection` — create Qdrant collection (#5)
- **Added** [api] `GET /api/ingest/collections` — list collections with stats (#5)
- **Added** [api] `DELETE /api/ingest/collection/{name}` — drop collection (#5)
- **Added** [api] `GET /api/health` — live status of qdrant, ollama, embed-sidecar (#5)
- **Added** [api] `ChunkingService` — sliding window 512 chars / 64 overlap with title prepending (#6)
- **Added** [api] `POST /api/ingest/documents` — chunk → embed → upsert pipeline (#7)
- **Added** [api] Three upsert modes: `skip_existing`, `overwrite`, `error_on_conflict` (#7)
- **Added** [api] Batched embedding via `INGEST_BATCH_SIZE` env config (#7)
- **Added** [ingest-cli] `queriva_ingest.py` CLI entry point (#9)
- **Added** [ingest-cli] JSON loader (`--format json`) (#9)
- **Added** [ingest-cli] CSV loader with column mapping (`--format csv --map ...`) (#9)
- **Added** [ingest-cli] File loader — recursive `.txt`, `.md`, `.pdf` (`--format files`) (#9)
- **Added** [ingest-cli] URL loader — fetch + extract text (`--format urls`) (#9)
- **Added** [ingest-cli] Stdin JSONL loader (`--format jsonl`) (#9)
- **Added** [infra] Demo seed fixture: `fixtures/news_radar_dhaka_floods.json` — 8 articles BN+EN (#11)
- **Added** [infra] `scripts/seed-demo.sh` — idempotent seed via ingest CLI (#11)

### Infrastructure
- **Infrastructure** [infra] Qdrant Docker service with persistent volume and healthcheck (#4)

### Docs
- **Docs** [adr] ADR-003 — Sliding window chunking with title prepending (before #6)
- **Docs** [adr] ADR-007 — Character-based over token-based chunking (before #6)
- **Docs** [adr] ADR-008 — Source document_id as upsert identity key (before #7)

---

## [0.3.0] — TBD
> Search + RAG: natural language queries return ranked results and AI summaries

### Added
- **Added** [api] `QdrantSearchService` — vector search with payload filters and min_score threshold (#12)
- **Added** [api] Chunk deduplication by `document_id` — top-k shows one result per source article (#12)
- **Added** [api] `QueryEmbeddingService` — HTTP client to embed-sidecar with 1-retry (#13)
- **Added** [api] `POST /api/search` (search mode) — embed → search → ranked results (#14)
- **Added** [api] Filters: `language`, `date_from`, `date_to`, `category` (#14)
- **Added** [api] `latency_ms` breakdown: `embed`, `search`, `total` in search mode (#14)
- **Added** [api] `LLMSynthesisService` — Ollama/Mistral RAG synthesis (#17)
- **Added** [api] `POST /api/search` (rag mode) — synthesized answer with article citations (#17)
- **Added** [api] `latency_ms.synthesis` in RAG mode response (#17)
- **Added** [api] `SEARCH_MAX_SCORE_AUTO_ACCEPT` — skip LLM for high-confidence direct hits (#17)
- **Added** [api] Graceful degradation when Ollama is unreachable (#17)

### Infrastructure
- **Infrastructure** [infra] Ollama Docker service with Mistral 7B model (#16)

### Docs
- **Docs** [adr] ADR-006 — Ollama + Mistral 7B over vLLM / llama.cpp (before #16)

---

## [0.4.0] — TBD
> Standalone UI: full search experience in the browser

### Added
- **Added** [ui] Design tokens: navy `#0D1B2A`, teal `#00C9B8`, amber `#F6AD55` palette (#19)
- **Added** [ui] Brand assets: icon, wordmark horizontal, wordmark stacked, favicon (#19)
- **Added** [ui] Tabler Icons integration (#19)
- **Added** [ui] Light + dark theme support via CSS variables (#19)
- **Added** [ui] `TopBar` — logo, Queriva brand, status pill (qdrant/ollama/labse), icons (#20)
- **Added** [ui] `SearchBar` — input with mode toggle (Search / RAG) (#20)
- **Added** [ui] `FilterStrip` — language, date range, category chips, collection badge (#20)
- **Added** [ui] `ResultCard` — rank, title, score bar, snippet, source, language badge, date (#21)
- **Added** [ui] `ResultsList` — top-result highlight, load more (#21)
- **Added** [ui] `StatsPanel` — hits, best score, languages, total time (#21)
- **Added** [ui] `AISummary` — header, body with reference badges, copy + refresh actions (#22)
- **Added** [ui] `LatencyFooter` — embed / search / llm breakdown (#22)
- **Added** [ui] `SuggestionsPanel` — follow-up query buttons (#22)
- **Added** [ui] `useSearch` hook — query, mode, filters, loading, error, response state (#23)
- **Added** [ui] Standalone SPA at port 3000 wired to full backend (#23)

---

## [0.5.0] — TBD
> MFE widget + full stack: one command to run, embeddable anywhere

### Added
- **Added** [ui] `SearchWidget` — Module Federation micro-frontend export (#25)
- **Added** [ui] `SearchWidgetProps` TypeScript API: `apiUrl`, `collection`, `theme`, `filters`, `onResultClick` (#25)
- **Added** [ui] Host example app at `packages/ui/examples/host/` (#25)

### Infrastructure
- **Infrastructure** [infra] `docker-compose.yml` full stack: qdrant, ollama, embed-sidecar, api, ui with healthchecks (#27)
- **Infrastructure** [infra] `docker-compose.dev.yml` with hot reload for api and ui (#27)
- **Infrastructure** [infra] `.env.example` with all config vars per SPEC §13 (#27)
- **Infrastructure** [infra] Playwright E2E smoke tests: ingest → search → RAG full journey (#28)
- **Infrastructure** [infra] `scripts/e2e-smoke.sh` — one-command E2E validation (#28)

### Docs
- **Docs** [adr] ADR-010 — Module Federation over iframe / Web Components (before #25)

---

## [1.0.0] — TBD
> Client-presentable: fresh clone to working demo in under 20 minutes

### Docs
- **Docs** [docs] `README.md` — quick start, architecture diagram, ingestion guide, MFE integration guide, troubleshooting (#29)
- **Docs** [docs] Configuration reference — all env vars with defaults and descriptions (#29)
- **Docs** [docs] Qdrant payload schema reference (#29)
- **Docs** [docs] News Radar → Queriva integration guide (#29)
- **Docs** [docs] Development guide: run packages individually, seed, test (#29)
