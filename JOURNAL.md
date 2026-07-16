# Queriva — Development Journal

Personal log of daily progress, blockers, and decisions.
One entry per working day. Most recent entry at the top.

---

## 2026-07-16 — MFE integration tests (#26)

### Shipped
- Issue #26 / GH #29 — `mfeExports` (`SearchWidgetProps`, `SearchResult`, `SearchMode`)
- Shared App ↔ SearchWidget surface tests; expanded SearchWidget props coverage
- Host `specConsumptionSample.tsx` (SPEC §11) + `typecheck`; `make test-mfe` in CI

### Next
- #27 — Full Docker Compose stack (ui service)

---

## 2026-07-16 — Module Federation SearchWidget (#25)

### Shipped
- Issue #25 / GH #28 — Vite Module Federation remote `queriva` exposes `./SearchWidget`
- `SearchWidgetProps` (SPEC §11): `apiUrl` + `collection` required; placeholder, mode, theme, filters, `onResultClick` optional
- Example host at `packages/ui/examples/host/` with `VITE_QUERIVA_REMOTE_ENTRY`
- `make build-mfe` builds remote + host; preview remote on port 5173
- Docs #23 manual AC already closed (GH #26); confirmation comment added

### Next
- #26 — MFE integration tests
- Manual: host + remote against docker compose API

---

## 2026-07-16 — Fix SEARCH_MIN_SCORE / empty demo results (#63)

### Shipped
- GitHub issue [#63](https://github.com/madmmas/queriva/issues/63) + PR #64 (`issue-63/search-min-score-env`)
- Wire `SEARCH_MIN_SCORE` into Compose `api` service; resolve omitted `min_score` from config
- UI reads `VITE_SEARCH_MIN_SCORE`; default threshold **0.40** for LaBSE + seeded fixture
- SPEC §6/§13 + `.env.example` aligned (0.60 → 0.40)
- Manual #23 verification: docker compose backend + Vite UI — `floods in Dhaka last week` returns hits
- README hero replaced with live standalone UI screenshot; status → v0.4.0
- Ignore `coverage/` in `.gitignore`; untrack `packages/ui/coverage/`

### Decisions
- LaBSE cosine scores on `news_radar` peak ~0.3–0.55; default `min_score` 0.60 hid all demo results
- `SEARCH_MIN_SCORE` alone is not enough for Vite — need `VITE_SEARCH_MIN_SCORE` (or code default)
- API image must be rebuilt (`--build`), not only `--force-recreate`, after Java changes

### Next
- Merge PR #64 / close #63
- #25 — Module Federation `SearchWidget` (ADR-010 first)

---

## 2026-07-04 — Release v0.4.0

### Shipped
- **Release v0.4.0** — Standalone UI (#19–#24)
  - Phase 6 complete: design tokens, search zone, results/stats, AI summary panel, `useSearch` hook, full test suite
  - Standalone SPA at `localhost:3000` wired to `POST /api/search` (search + RAG modes)
  - Git tag `v0.4.0` + GitHub release
  - Upwork milestone: UI demo + blog post (SPEC §18)

### Next
- #25 — Module Federation `SearchWidget` (ADR-010 first)

---

## 2026-07-04 (continued — #24)

### Plan
- [x] #24 — Standalone UI tests

### Built
- **Issue #24** — Standalone UI tests (`issue-24/standalone-ui-tests`)
  - MSW handlers with search-mode/RAG response builder and request capture helper
  - Vitest `env` override — `VITE_API_URL=''` so MSW intercepts regardless of local `.env`
  - App E2E tests: search flow, RAG summary show/hide, filter chip → API params, loading, error, axe
  - Component tests: `SearchBar`, `LoadMoreButton`, `SuggestionsPanel`, `StatsPanel`
  - `useSearch` tests: filters, loadMore; handlers unit tests for both modes
  - Coverage: components ~95%, hooks ~85%, overall ≥80%

### Decisions
- Test env forces empty API base URL — dev `.env` with `localhost:8080` must not leak into Vitest

### Blockers
None.

### Next
- #25 — Module Federation `SearchWidget` (ADR-010 first)

---

## 2026-07-04 (continued)

### Plan
- [x] #23 — Standalone app + useSearch hook

### Built
- **Issue #23** — Standalone app + useSearch (`issue-23/use-search-hook`)
  - `useSearch` — `useReducer` with idle/loading/success/error; `POST /api/search` via `searchApi.ts`
  - Mode and filter changes re-run search; load-more increments `top_k`; refresh re-submits current query
  - `ErrorBanner` for API failures; `App.tsx` wired to hook (no demo fixture state)
  - MSW handlers for `/api/search` (search + RAG modes); Vite dev server port 3000 per SPEC §12
  - RTL + MSW tests for search flow, RAG summary, loading skeleton, and 500 error banner

### Decisions
- Omit `AbortSignal` on fetch — jsdom + MSW reject non-native signals in Vitest
- `SearchBar` passes current query string to `onSubmit` to avoid stale-state race on Enter

### Blockers
None.

### Next
- #24 — Standalone UI tests (full coverage + axe audit)

---

## 2026-07-04

### Plan
Today's scope — phase 6 UI results + RAG panel (#21–#22):
- [x] #21 — Results list + stats components
- [x] #22 — AI summary panel

### Built
- **Issue #21** — Results list + stats (`issue-21/results-list-stats`, PR #58)
  - `ResultCard` — rank, title, score bar (dynamic `width` inline style), snippet, source, BN/EN badge, date; optional `onClick` for widget mode
  - `ResultsList` — "Matching articles" section, top-result highlight on first two cards, `EmptyState` and `SearchSkeleton` states
  - `LoadMoreButton` — increments visible `top_k` in App demo layout
  - `StatsPanel` — hits, best score, language count, total time metric cards
  - `formatters.ts` + `searchStats.ts` utilities; demo fixture in `src/fixtures/demoSearchResponse.ts`
  - RTL tests for `ResultCard`, `ResultsList`, formatters, searchStats; 90%+ UI coverage
- **Issue #22** — AI summary panel (`issue-22/ai-summary-panel`)
  - `AISummary` — RAG header, inline `[N]` reference badges, copy + refresh actions
  - `LatencyFooter` — embed / search / llm breakdown with ms and seconds formatting
  - `SuggestionsPanel` — three follow-up query buttons from mock UI
  - `RagPanel` — hides in search mode or when `summary` is null; wired in App right column
  - `demoRagSearchResponse` fixture; RTL tests for visibility, clipboard copy, refresh callback

### Decisions
- Moved demo mock data from `__tests__/` to `src/fixtures/` so `App.tsx` does not import test-only paths
- `StatsPanel` renders stats grid only; App owns the `qv-right` aside wrapper for stats + RAG panel

### Blockers
None.

### Next
- #23 — Standalone app + `useSearch` hook (wire search API end-to-end)

---

## 2026-07-03

### Plan
Today's scope — finish phase 5 RAG (#12–#18) and phase 6 UI search zone (#19–#20):
- [x] #16 — Ollama Docker setup
- [x] #17 — RAG mode (LLMSynthesisService)
- [x] #18 — RAG mode integration tests
- [x] #19 — UI scaffold + design tokens + brand assets
- [x] #20 — Search zone components

### Built
- **Issue #16** — Ollama Docker setup (`issue-16/ollama-docker-setup`, PR #52)
  - `ollama` service in `docker-compose.yml` — `ollama/ollama:0.5.7`, port 11434, `ollama_data` volume, healthcheck on `/api/tags`
  - `scripts/ollama-pull-model.sh` + `make ollama-pull` for first-run `mistral` model download (~4GB)
  - API wired to `OLLAMA_URL=http://ollama:11434`; `OLLAMA_MODEL=mistral` in compose and `application.yml`
  - `OllamaHealthIT` — health reports `connected` when `/api/tags` responds, `disconnected` on failure
- **Issue #17** — RAG mode (`issue-17/rag-mode`, PR #54)
  - `LLMSynthesisService` — Ollama `POST /api/generate` with SPEC §10 prompt; graceful degradation on 503/unreachable
  - `RagPromptBuilder` + `RagSynthesisConstants` — numbered articles, system instructions, user question
  - `SearchService` RAG path — `mode=rag` populates `summary` and `latency_ms.synthesis`; skips LLM when top score ≥ `SEARCH_MAX_SCORE_AUTO_ACCEPT` (0.80)
  - `SearchResultMapper.toRagResponse()` — RAG response shape per SPEC §6
  - WireMock unit tests (`LLMSynthesisServiceTest`, `RagPromptBuilderTest`); optional `LLMSynthesisServiceSidecarIT` (`@Tag("slow")`)
- **Issue #18** — RAG integration tests (`issue-18/rag-integration-tests`)
  - `RagModeIT` — full embed → search → synthesize pipeline via `POST /api/search` with WireMock Ollama (`make test-int`)
  - `RagModeSlowIT` — live Ollama validation (`@Tag("slow")`, `make test-slow`); baseline ~2–5s per SPEC §15
  - **Release v0.3.0** — search + RAG via API complete (#12–#18)
- **Issue #19** — UI scaffold (`issue-19/ui-scaffold`)
  - Brand SVG assets in `packages/ui/public/` + `favicon.svg`
  - `src/styles/tokens.css` — `--qv-navy`, `--qv-teal`, `--qv-amber` + light/dark semantic surfaces
  - `src/types/api.ts` — SPEC §6 TypeScript contracts; `src/constants/ui.ts` — UI strings
  - `@tabler/icons-react` 3.44.0; `useTheme` hook; scaffold `App` with top bar and token swatches
  - `npm run typecheck` (`tsc -b --noEmit`); Vitest coverage for api types, tokens, theme
- **Issue #20** — Search zone components (`issue-20/search-zone-components`)
  - `TopBar` — health status pill via `useHealth` + MSW; settings/menu icons with `aria-label`
  - `SearchBar` — labelled input, Enter/submit, embeds `ModeToggle`
  - `FilterStrip` — language, last-7-days, category chips + amber collection badge; `ResultCount`
  - Mock-aligned CSS at 1280px (`max-width: 80rem`); RTL + axe-core tests

### Blocked
- Nothing

### Decided
- Ollama does not block `status=ok` on `/api/health` — same as pre-Docker behaviour (#5)
- Model pull is explicit (`make ollama-pull`), not automatic on `docker compose up` — avoids surprise 4GB download
- LLM request body serialized via injected `ObjectMapper` — bare `RestClient.builder()` lacks Jackson converters in unit tests
- RAG integration tests raise `search.max-score-auto-accept` to 0.99 so ranked fixture hits still trigger synthesis
- Design token values verified in unit test by reading `tokens.css` — jsdom does not resolve CSS custom properties in `getComputedStyle`
- Collection badge is display-only (not a filter chip); filter state maps to SPEC §6 via `toSearchFilters`

### Tomorrow
- Issue #21 — Results list + stats components

---

## 2026-07-02

### Plan
Today's scope — issues **#12–#18** (phase 4 search + phase 5 RAG):
- [x] #12 — QdrantSearchService
- [x] #13 — QueryEmbeddingService
- [x] #14 — POST /api/search (search mode)
- [x] #15 — Search mode integration tests
- [ ] #16 — Ollama Docker setup (completed 2026-07-03 — see that entry)
- [ ] #17 — RAG mode (LLMSynthesisService)
- [ ] #18 — RAG mode integration tests

### Built
- **Issue #12** — QdrantSearchService (`issue-12/qdrant-search-service`)
  - `QdrantSearchService.search()` — vector search with typed filters, min-score cutoff, and document-level dedup
  - `QdrantSearchFilterBuilder` — language, category, `published_at` date range; excludes `queriva_internal` metadata points
  - `SearchHit`, `SearchFilters`, `SearchConstants` records/constants per SPEC §6
  - Over-fetch `topK × 5` chunk points; keep highest-scoring chunk per `document_id`
  - Unit tests (mocked Qdrant client) + integration tests (Testcontainers + fixture ingest via API)
- **Issue #13** — QueryEmbeddingService (`issue-13/query-embedding-service`)
  - `QueryEmbeddingService.embed()` — POST embed-sidecar `/api/embed` with 5s/30s timeouts and one retry on 5xx/I/O
  - `validateEmbeddingModel()` delegates to `CollectionEmbeddingModelService.validateModelForSearch()` (400 on mismatch)
  - Default model from `EMBED_DEFAULT_MODEL`; WireMock unit tests for 200/503 retry/400/timeout paths
  - Optional integration test against running embed-sidecar (LaBSE 768-dim)
- **Issue #14** — POST /api/search search mode (`issue-14/search-api`)
  - `SearchController` + `SearchService` — embed → search → map pipeline per SPEC §8
  - `SearchRequest`/`SearchResponse`/`SearchLatencyMs` records with SPEC §6 defaults and validation
  - `summary=null`, `latency_ms.synthesis` omitted in search mode; query logged at DEBUG only
  - `@WebMvcTest` validation tests + `SearchPipelineIT` (Testcontainers + fixture corpus)
- **Issue #15** — Search mode integration tests (`issue-15/search-mode-integration-tests`)
  - `SearchModeIT` — ranked top result, `top_k`, `min_score`, bn/en filters, date range, <500ms baseline
  - `SearchIntegrationTestSupport` — shared WireMock embed stubs; `SearchPipelineIT` refactored to reuse

### Blocked
- Nothing

### Decided
- `SearchHit.id` maps from Qdrant payload `document_id`, not chunk point ID
- Date filters accept `YYYY-MM-DD` (expanded to start/end of day) or full ISO timestamps

### Tomorrow
- Issue #19 — UI scaffold + design tokens + brand assets
- Issue #20 — Search zone components
- Issue #21 — Results list + stats components
- Issue #22 — AI summary panel

---

## 2026-07-01

### Built
- **Issue #11** — Seed demo data (`issue-11/seed-demo-data`)
  - `scripts/validate_fixture.py` — validates 8 articles (4 bn + 4 en) with SPEC §14 source fields
  - `scripts/seed-demo.sh` — point-count check (≥8), idempotent re-ingest verification
  - `docker-compose.yml` extended with embed-sidecar + api for ingest stack
  - API multi-stage Dockerfile; README quick start (`docker compose up` → `make seed`)
  - Released **v0.2.0** — issues #4–#11 complete
- **Issue #9** — Ingest CLI (`issue-9/ingest-cli`)
  - `queriva_ingest.py` — argparse CLI posting to `POST /api/ingest/documents` (SPEC §7.3)
  - Loaders: JSON, CSV (`--map`), recursive files (`.txt`/`.md`/`.pdf`), URLs, JSONL stdin
  - `api_client.py` via httpx; path traversal + SSRF protections in file/url loaders
  - 21 unit tests (83% coverage); integration test against running API when available
- **Issue #10** — Ingest CLI tests (`issue-10/ingest-cli-tests`)
  - Parametrized loader tests for json, csv, jsonl, files; shared fixtures in `conftest.py`
  - CLI subprocess tests: missing args, invalid format, unknown flags, API unreachable
  - HTTP error coverage: 400/409/500 + connection refused via pytest-httpx
  - Security: SSRF (10.x, 172.16, 192.168, loopback), path traversal `../../../etc/passwd`
  - 67 unit tests, 99% coverage; `make test-ingest` passes with no network

### Blocked
- Nothing

### Decided
- Docker compose scoped to ingest stack (qdrant + embed-sidecar + api); Ollama/ui deferred to #16/#27
- Seed script validates fixture locally before any API calls

### Tomorrow
- Issue #12 — QdrantSearchService
- Issue #13 — QueryEmbeddingService
- Issue #14 — POST /api/search (search mode)
- Issue #15 — Search mode integration tests
- Issue #16 — Ollama Docker setup
- Issue #17 — RAG mode (LLMSynthesisService)
- Issue #18 — RAG mode integration tests

---

## 2026-06-30

### Built
- **Issue #8** — Ingest API integration tests (`issue-8/ingest-api-integration-tests`)
  - `IngestPipelineIT` — 6 tests against Testcontainers Qdrant + WireMock embed sidecar
  - Shared `NewsRadarFixtureSupport` loads `fixtures/news_radar_dhaka_floods.json` (8 articles, 13 chunks)
  - Covers: point count, chunking on/off, SPEC §14 payload fields, bn/en languages, `skip_existing` idempotency, <30s baseline
  - `QdrantRestTestSupport` for Qdrant REST scroll assertions; removed ad-hoc `IngestServiceIT`
- **Issue #7** — Ingest API (`issue-7/ingest-api`)
  - `POST /api/ingest/documents` — chunk → embed → upsert pipeline (SPEC §7.1–§7.2, §7.5, ADR-008)
  - Upsert modes: `skip_existing`, `overwrite`, `error_on_conflict`; batched embed calls (`INGEST_BATCH_SIZE=32`)
  - `EmbedSidecarClient`, `QdrantIngestRepository`, `CollectionEmbeddingModelService` for model validation
  - Qdrant payload per SPEC §14; overwrite deletes by `document_id` filter before re-upsert
  - Unit tests (`IngestServiceTest`, `IngestDocumentsControllerTest`); Testcontainers IT (`IngestPipelineIT` in #8)
- **Issue #6** — ChunkingService (`issue-6/chunking-service`)
  - Character-based sliding window: 512 chars / 64 overlap (ADR-003, ADR-007)
  - Title prepended to embed input; `body_snippet` capped at 500 chars without title
  - `Document` and `Chunk` records; point IDs `{document_id}-chunk-{N}`
  - 10 unit tests including golden-file coverage of `fixtures/news_radar_dhaka_floods.json` article #1
- **Issue #5** — API skeleton + health + collection management
  - `GET /api/health` — probes Qdrant (gRPC), Ollama, embed-sidecar; returns `ok` or `degraded`
  - `POST /api/ingest/collection`, `GET /api/ingest/collections`, `DELETE /api/ingest/collection/{name}`
  - `CollectionManager` via `io.qdrant:client` 1.14.1; `GlobalExceptionHandler`, CORS, config beans
  - `@WebMvcTest` unit tests (health + collection endpoints); Testcontainers ITs for create/list/delete
  - `packages/api/Dockerfile` with curl healthcheck
- **Issue #4** — Qdrant Docker service (`issue-4/qdrant-docker-service`)
  - `docker-compose.yml` with qdrant on 6333/6334, `qdrant_data` volume, curl healthcheck
  - `infra/docker/qdrant.Dockerfile` — curl added to official image for `/healthz` probe
  - `QdrantTestcontainersSupport` + expanded `QdrantContainerIT` (healthz, collections)
- **Issue #3** — embed sidecar full test coverage (`issue-3/embed-sidecar-test-coverage`)
  - Concurrent embed requests — model loaded only once (thread-safe loader)
  - Parametrized invalid model name → 422; dimension mismatch → 500
  - Health endpoint SPEC contract test; `model_loader` unit tests
  - Coverage floor raised to 90% in `pytest.ini`
- **Issue #2** — embed sidecar (`issue-2/embed-sidecar`, PR #35)
  - `POST /api/embed` and `GET /api/health` per SPEC §6 and §9
  - `model_loader.py` — lazy cache for LaBSE (768), MiniLM (384), multilingual-mpnet (768)
  - `config.py`, `Dockerfile`, pinned sentence-transformers + torch
  - Unit tests with mocked `SentenceTransformer` — 12 passed, 96% coverage

### Blocked
- Nothing

### Decided
- Empty document body produces **0 chunks** — nothing to embed or upsert (#6)
- Released **v0.1.0** — issues #0–#3 complete; CHANGELOG entries moved from `[Unreleased]` (#meta–#3)
- Qdrant Java client gRPC port derived from `QDRANT_URL` host + `QDRANT_GRPC_PORT` (6334) — REST URL stays 6333 (#5)
- Health `status=ok` requires Qdrant + embed-sidecar; Ollama reported but does not block ok (#5)
- Surefire `-Dnet.bytebuddy.experimental=true` for local Java 25 + Mockito compatibility (#5)
- Unit tests mock `SentenceTransformer` — real model download deferred to issue #3 (#2)
- Sync `def` routes for blocking `encode()` — FastAPI thread pool (#2)
- Model names validated with `MODEL_NAME_PATTERN` — spaces and slashes return 422 before 503 (#3)
- Real model download tests stay out of CI — mocked `SentenceTransformer` keeps suite under 60s (#3)
- Thin Dockerfile wrapper for curl — upstream `qdrant/qdrant` image ships without curl (#3491)
- Compose file scoped to qdrant only; full stack deferred to issue #27

### Tomorrow
- Issue #9 — Ingest CLI
- Issue #10 — Ingest CLI tests
- Issue #11 — Seed demo data
- Issue #12 — QdrantSearchService
- Issue #13 — QueryEmbeddingService
- Issue #14 — POST /api/search (search mode)
- Issue #15 — Search mode integration tests
- Issue #16 — Ollama Docker setup
- Issue #17 — RAG mode (LLMSynthesisService)
- Issue #18 — RAG mode integration tests

---

## 2026-06-29

### Built
- **Foundation** — GitHub repo `madmmas/queriva`, SPEC, ISSUES, all 12 ADRs, Cursor rules, fixture, seed script
- **Issue #0** — Turborepo monorepo scaffold (`packages/api`, `embed-sidecar`, `ingest-cli`, `ui`); `turbo run build` exits 0
- **Issue #1** — test infrastructure across all packages (JUnit/Testcontainers/WireMock, pytest, Vitest/MSW/jest-axe); `make test` exits 0
- **Issue #1b** — GitHub Actions CI (`.github/workflows/ci.yml`); `make install` → `build` → `test` on every PR/push to `main`
- **README** — Queriva SVG icon + centered header (PR #31)

### Blocked
- UI jest-axe duplicate `<main>` landmark — fixed with `afterEach(cleanup)`
- WireMock `wiremock-junit5` not on Maven Central — switched to `wiremock-standalone`
- Python `test-int` coverage gate on partial runs — use `--no-cov` for integration-only runs
- macOS has no `python` shim — all scripts use `python3 -m pip`

### Decided
- Fresh repo rather than retrofitting the old one
- Single foundation commit for all ADRs at repo bootstrap
- Python packages use `python3` in npm/turbo scripts
- Integration test runs skip coverage; enforced on `test` and `test-unit` only
- Branch protection on `main` — require `CI / Build and test` (manual GitHub settings)

### Tomorrow
- Issue #2 — embed sidecar
- Issue #3 — embed sidecar full test coverage (90%+)
- Issue #4 — Qdrant Docker service
- Issue #5 — API skeleton + health + collection management
- Issue #6 — ChunkingService
- Issue #7 — Ingest API
- Issue #8 — Ingest API integration tests


---

Format:
```
## YYYY-MM-DD
### Built
### Blocked
### Decided
### Tomorrow
```

<!--
TIPS:
- Write this at end of day, takes 5 minutes
- "Blocked" appearing 3+ days in a row = stop and resolve it
- "Decided" entries are seeds for blog posts
- "Tomorrow" is your morning standup with yourself
-->
