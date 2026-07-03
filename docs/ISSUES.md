# Queriva — Implementation Issues

> Sole developer: Moinuddin Masud (@madmmas)
> Derived from: `docs/SPEC.md`, `docs/adr/`, `.cursor/rules/`, mock UI, brand assets.
>
> **Workflow:** one issue at a time, in dependency order. An issue is not done
> until all tasks are complete, all tests pass, CHANGELOG.md is updated, and
> JOURNAL.md has an entry for the day. See `.cursor/rules/queriva.mdc` Rule 3.
>
> **Last updated:** v3 — added ADR issues, repo foundation files, security rules,
> fixture file, README template, release issues. Aligned with SPEC v3 (§18
> version guide, §19 ADR index).

---

## Dependency graph

```
#meta ADR issues (write before their mapped implementation issue — see ADR column)
  │
#0  Scaffold monorepo + repo foundation files
  └─► #1  Test infrastructure ──► #1b GitHub Actions CI
        └─► #2  Embed sidecar ──► #3  Embed sidecar tests
              └─► #4  Qdrant Docker service
                    └─► #5  API skeleton + health + collection management
                          ├─► #6  ChunkingService
                          │     └─► #7  Ingest API ──► #8  Ingest API integration tests
                          │               ├─► #9  Ingest CLI ──► #10 Ingest CLI tests
                          │               └─► #11 Seed demo data
                          ├─► #12 QdrantSearchService
                          └─► #13 QueryEmbeddingService
                                └─► #14 Search API (search mode) ──► #15 Search integration tests
                                      └─► #16 Ollama Docker setup
                                            └─► #17 RAG mode ──► #18 RAG integration tests
                                                  └─► #19 UI scaffold + design tokens
                                                        ├─► #20 Search zone components
                                                        ├─► #21 Results + stats components
                                                        └─► #22 AI summary panel
                                                              └─► #23 Standalone App ──► #24 UI tests
                                                                    └─► #25 MFE widget ──► #26 MFE tests
                                                                          └─► #27 Full Docker Compose
                                                                                └─► #28 E2E smoke tests
                                                                                      └─► #29 README + docs
                                                                                            └─► #30 Release v1.0.0
```

**ADR → issue dependency (write ADR before starting the mapped issue):**

| ADR | Write before issue |
|---|---|
| ADR-001 Qdrant | #0 |
| ADR-004 Spring Boot | #0 |
| ADR-005 FastAPI sidecar | #0 |
| ADR-011 Ingest in Spring Boot | #0 |
| ADR-012 Turborepo | #0 |
| ADR-009 Testcontainers | #1 |
| ADR-002 LaBSE | #2 |
| ADR-006 Ollama + Mistral | #2 |
| ADR-003 Chunking strategy | #6 |
| ADR-007 Char-based chunking | #6 |
| ADR-008 document_id identity | #7 |
| ADR-010 Module Federation | #25 |

---

## Issue #meta — Write all ADRs before mapped implementation issues

**Labels:** `adr`, `docs`, `phase-0`
**Depends on:** —
**Blocks:** #0 (partially — ADR-001/004/005/011/012 must be done first)

### Description

All 12 ADRs must be written before the implementation issue they are mapped to.
This issue tracks the ADR writing work as a group. Each ADR is a separate
task below. See `docs/SPEC.md §19` for the full ADR index.

### Tasks

**Before #0:**
- [x] `docs/adr/ADR-001-qdrant.md` — Qdrant over Weaviate, Milvus, pgvector, Chroma
- [x] `docs/adr/ADR-004-spring-boot.md` — Spring Boot / Java 21 over Go, FastAPI
- [x] `docs/adr/ADR-005-embed-sidecar.md` — FastAPI sidecar over ONNX-in-JVM
- [x] `docs/adr/ADR-011-ingest-in-api.md` — Ingest orchestration in Spring Boot
- [x] `docs/adr/ADR-012-turborepo.md` — Turborepo over Nx

**Before #1:**
- [x] `docs/adr/ADR-009-testcontainers.md` — Testcontainers over mock Qdrant client

**Before #2:**
- [x] `docs/adr/ADR-002-labse.md` — LaBSE as default embedding model
- [x] `docs/adr/ADR-006-ollama-mistral.md` — Ollama + Mistral 7B over vLLM / llama.cpp

**Before #6:**
- [x] `docs/adr/ADR-003-chunking-strategy.md` — Sliding window with title prepending
- [x] `docs/adr/ADR-007-char-chunking.md` — Character-based over token-based chunking

**Before #7:**
- [x] `docs/adr/ADR-008-document-id.md` — Source document_id as upsert identity key

**Before #25:**
- [x] `docs/adr/ADR-010-module-federation.md` — Module Federation over iframe / Web Components

### Acceptance criteria

- All 12 ADR files exist in `docs/adr/` with correct naming
- Each ADR has: Status, Context, Decision, Alternatives Considered, Consequences, References
- Each ADR references the SPEC section(s) and issue(s) it relates to
- CHANGELOG.md has a `Docs [adr]` entry for each ADR

### Test plan

- [x] `ls docs/adr/ | wc -l` returns 12
- [x] Each file passes markdown lint (no broken headings or tables)

---

## Issue #0 — Scaffold monorepo + repo foundation files

**Labels:** `infra`, `phase-0`
**Depends on:** ADR-001, ADR-004, ADR-005, ADR-011, ADR-012 (write first)
**Blocks:** #1, #2, #5, #9, #19

### Description

Bootstrap the Turborepo monorepo per SPEC §5. Commit all repo foundation files
(Cursor rules, repo docs, fixture, seed script) in this issue. No business
logic yet — only structure, tooling, and repo hygiene.

### Tasks

**Monorepo structure:**
- [x] Root `package.json`, `turbo.json` per SPEC §5
- [x] `packages/api/` — Spring Boot 3.x / Java 21, Maven, `search/` and `ingest/` stubs
- [x] `packages/embed-sidecar/` — Python 3.11+, FastAPI skeleton
- [x] `packages/ingest-cli/` — Python 3.11+, CLI skeleton + `loaders/` dir
- [x] `packages/ui/` — React 18, Vite, TypeScript scaffold
- [x] Root `Makefile` with all required targets (see `queriva.mdc` Rule 11)
- [x] `.gitignore` covering: `.env`, `target/`, `dist/`, `__pycache__/`, `node_modules/`, `*.pyc`, `*.jar`, model weights

**Repo foundation files:**
- [x] `LICENSE` — MIT
- [x] `README.md` stub — one paragraph + link to SPEC + "implementation in progress" note
- [x] `CONTRIBUTING.md` — sole developer workflow (branch naming, commit format, what not to commit)
- [x] `SECURITY.md` — scope, private reporting link, known limitations by design
- [x] `JOURNAL.md` — daily log template (first entry: today's date + "Scaffolded monorepo")
- [x] `README-TEMPLATE.md` — full README template Cursor will use for issue #29
- [x] `CHANGELOG.md` — initial structure with all version sections pre-populated (v0.1.0 → v1.0.0)

**Cursor rules:**
- [x] `.cursor/rules/queriva.mdc` — workflow rules (20 rules)
- [x] `.cursor/rules/code-quality.mdc` — code quality rules (Java, Python, TypeScript, security)
- [x] `.cursor/rules/test-quality.mdc` — test quality rules (Java, Python, TypeScript, E2E)

**Fixture and seed:**
- [x] `fixtures/news_radar_dhaka_floods.json` — 8 articles (4 BN + 4 EN), full SPEC §14 payload fields
- [x] `scripts/seed-demo.sh` — idempotent curl-based seed script

**Mock UI and brand assets:**
- [x] `mock-ui/queriva_mock_ui_v2.html` — interactive design reference (open in browser)
- [x] `mock-ui/queriva-icon-files/` — all 8 SVG brand assets
- [x] `mock-ui/README.md` — one paragraph explaining these are design references,
      not production assets; production assets move to packages/ui in issue #19

**SPEC and docs:**
- [x] `docs/SPEC.md` — current version (v3 with §18 version guide, §19 ADR index)
- [x] `docs/ISSUES.md` — this file
- [x] `docs/adr/` directory — empty, ready for ADR files from #meta

### Acceptance criteria

- `turbo run build` exits 0 from repo root
- Directory layout matches SPEC §5 exactly
- All foundation files committed and present
- `.cursor/rules/` has all three `.mdc` files
- `fixtures/news_radar_dhaka_floods.json` has 8 valid JSON documents
- `.gitignore` prevents `.env`, build artifacts, and binary files
- `mock-ui/` contains the HTML mock and all 8 SVG brand assets

### Test plan

- [x] `turbo run build` exits 0
- [x] `ls .cursor/rules/` shows three `.mdc` files
- [x] `python -c "import json; data=json.load(open('fixtures/news_radar_dhaka_floods.json')); assert len(data)==8"`
- [x] `git status` after `make build` shows no untracked artifacts
- [x] CHANGELOG.md entry added for this issue

---

## Issue #1 — Set up test infrastructure

**Labels:** `testing`, `infra`, `phase-0`
**Depends on:** #0, ADR-009 (write first)
**Blocks:** #1b, #3, #8, #10, #15, #18, #24, #26, #28

### Description

Establish testing conventions and tooling across all packages. Every subsequent
issue depends on this being solid. See `test-quality.mdc` for conventions.

### Tasks

- [x] **API:** JUnit 5 + Spring Boot Test + Testcontainers (Qdrant) config — see ADR-009
- [x] **API:** AssertJ dependency added to `pom.xml`
- [x] **API:** WireMock dependency added for HTTP client unit tests
- [x] **Embed sidecar:** pytest + httpx AsyncClient + pytest-cov + respx
- [x] **Ingest CLI:** pytest + pytest-httpx + subprocess test pattern
- [x] **UI:** Vitest + React Testing Library + MSW + jest-axe
- [x] Test tag conventions documented in each package: `@Tag("unit")`, `@Tag("integration")`, `@Tag("slow")`
- [x] `turbo run test` pipeline configured
- [x] All Makefile test targets working: `make test`, `make test-unit`, `make test-int`, `make test-api`, `make test-embed`, `make test-ingest`, `make test-ui`
- [x] Each package has at least one passing placeholder test

### Acceptance criteria

- `make test-unit` exits 0, no Docker required
- `make test-int` exits 0, Docker required
- Coverage reporting works in embed-sidecar, ingest-cli, and ui
- All Makefile targets defined

### Test plan

- [x] `make test` from root exits 0
- [x] `make test-unit` runs without Docker daemon
- [x] Coverage report generated for Python packages

---

## Issue #1b — GitHub Actions CI pipeline

**Labels:** `ci`, `infra`, `phase-0`
**Depends on:** #1
**Blocks:** —

### Description

CI on every PR and push to `main`. Uses Makefile targets exclusively.

### Tasks

- [x] `.github/workflows/ci.yml` — trigger on `pull_request` and `push` to `main`
- [x] Matrix: Node 20, Java 21, Python 3.11 on `ubuntu-latest`
- [x] Steps: `make install` → `make build` → `make test`
- [x] Docker available for Testcontainers (`ubuntu-latest` supports this natively)
- [x] Cache: `~/.npm`, `~/.m2`, pip cache
- [x] CI badge in `README.md` stub
- [x] Branch protection: require CI green before merge to main (configure in GitHub settings)

### Acceptance criteria

- CI passes on `main`
- Deliberate test failure on a branch → CI blocks
- Testcontainers connects to Docker in Actions log

### Test plan

- [x] Push to a branch → CI runs and passes
- [x] Break a test → CI fails with clear error
- [x] Check Actions log confirms Docker + Testcontainers step

---

## Issue #2 — Implement embed sidecar

**Labels:** `embed-sidecar`, `phase-1`
**Depends on:** #0, #1, #1b, ADR-002 + ADR-006 (write first)
**Blocks:** #3, #7, #13

### Description

FastAPI embedding service shared by ingest and search paths (SPEC §9, ADR-005).

### Tasks

- [x] `POST /api/embed` — `{ text, model }` → `{ vector, dimensions }`
- [x] `GET /api/health` — `{ status: "ok" }`
- [x] Lazy model loading with in-memory cache per model name
- [x] Supported models: `LaBSE` (768-dim, default), `all-MiniLM-L6-v2` (384-dim), `paraphrase-multilingual-mpnet-base-v2` (768-dim)
- [x] Pydantic request/response models (code-quality.mdc C2)
- [x] Type hints on all functions (code-quality.mdc C1)
- [x] `logging` module only — no `print()` (code-quality.mdc C5)
- [x] All constants at module top (code-quality.mdc C3)
- [x] `requirements.txt` pinned exact versions (code-quality.mdc E6)
- [x] `Dockerfile`
- [x] Config via `os.getenv` with defaults: `DEFAULT_MODEL`, `PORT=8001`
- [x] Invalid model name → 503 with actionable error message (code-quality.mdc A7)

### Acceptance criteria

- `POST /api/embed {"text":"hello","model":"LaBSE"}` returns 768-dim vector
- Model loads once per name (verify via log: "Loaded model 'LaBSE'")
- All three models return correct dimensions
- Invalid model name returns 503 with message explaining how to fix

### Test plan

- [x] Unit: all three models return correct dimension counts
- [x] Unit: empty text handling
- [x] Unit: invalid model name returns 503
- [x] Contract test: request/response JSON matches SPEC §6
- [x] Manual: `curl -X POST localhost:8001/api/embed -d '{"text":"test","model":"LaBSE"}'`

---

## Issue #3 — Embed sidecar full test coverage

**Labels:** `testing`, `embed-sidecar`, `phase-1`
**Depends on:** #2
**Blocks:** #4

### Description

Harden coverage to ≥ 90% before ingest and search depend on the sidecar.

### Tasks

- [x] Parametrized test: all three models × correct dimensions (test-quality.mdc C3)
- [x] Test empty text input
- [x] Test concurrent requests — model loaded only once
- [x] Test health endpoint shape
- [x] Test model name with spaces / invalid characters → 422
- [x] `pytest --cov` meets 90% threshold

### Acceptance criteria

- ≥ 90% line coverage on `main.py` and `models.py`
- Tests run in < 60s without GPU

### Test plan

- [x] `make test-embed` passes with coverage report
- [x] All edge cases have descriptive test names per convention

---

## Issue #4 — Qdrant Docker service

**Labels:** `infra`, `qdrant`, `phase-1`
**Depends on:** #2, #3
**Blocks:** #5

### Description

Runnable Qdrant instance in Docker with healthcheck (SPEC §12, ADR-001).

### Tasks

- [x] `qdrant` service in `docker-compose.yml` — ports 6333/6334, persistent volume
- [x] `healthcheck` block: `curl -f http://localhost:6333/healthz`
- [x] Testcontainers Qdrant config validated in API test module
- [x] Document ports and access in `docker-compose.yml` comments

### Acceptance criteria

- `docker compose up qdrant` starts with healthcheck passing
- `curl localhost:6333/collections` returns 200

### Test plan

- [x] Manual: `curl localhost:6333/collections`
- [x] Testcontainers config verified by running placeholder API integration test

---

## Issue #5 — API skeleton + health + collection management

**Labels:** `api`, `ingest`, `phase-2`
**Depends on:** #0, #1, #4
**Blocks:** #6, #7, #12, #13

### Description

Spring Boot gateway skeleton with health and collection management (SPEC §6 health,
§7.2 collection API, ADR-004, ADR-011).

### Tasks

- [x] Spring Boot 3.x app: `dev.queriva.QuerivaApplication` + `search/` + `ingest/` packages
- [x] `GlobalExceptionHandler` via `@ControllerAdvice` (code-quality.mdc B8)
- [x] `GET /api/health` — actively checks Qdrant, Ollama, embed-sidecar (code-quality.mdc F4)
- [x] `POST /api/ingest/collection` — create collection (`vector_size`, `distance`, `recreate_if_exists`)
- [x] `GET /api/ingest/collections` — list with stats
- [x] `DELETE /api/ingest/collection/{name}` — returns 204
- [x] `CollectionManager.java` — Qdrant collection CRUD using typed client filter builder (code-quality.mdc E5)
- [x] Bean Validation on all request DTOs (code-quality.mdc B6)
- [x] Collection name validated: `^[a-zA-Z0-9_]{1,64}$` (code-quality.mdc E2)
- [x] Constructor injection only (code-quality.mdc B3)
- [x] Config via `@Value` with defaults matching `.env.example` (code-quality.mdc F5)
- [x] CORS: `localhost:3000`, `localhost:5173`, `CORS_ALLOWED_ORIGINS` env (queriva.mdc Rule 12)
- [x] `Dockerfile` for API package
- [x] All config keys reference SPEC §13 in comments

### Acceptance criteria

- Health returns `"ok"` when Qdrant + embed sidecar healthy; degrades gracefully when down
- Create `news_radar` collection (768-dim, Cosine) → list → delete cycle works
- Invalid collection name (special chars, >64 chars) returns 400

### Test plan

- [x] `@WebMvcTest` for health endpoint — mock dependency checks
- [x] `@WebMvcTest` for collection endpoints — validate request/response shapes
- [x] Integration test (Testcontainers Qdrant): create → list → delete
- [x] Integration test: health reflects real Qdrant status
- [x] CHANGELOG.md entry added

---

## Issue #6 — ChunkingService

**Labels:** `api`, `ingest`, `phase-2`
**Depends on:** #5, ADR-003 + ADR-007 (write first)
**Blocks:** #7

### Description

Sliding-window character-based chunker with title prepending (SPEC §7.4,
ADR-003, ADR-007). Character-based (not token-based) — this is decided in ADR-007.

### Tasks

- [x] `ChunkingService.chunk(Document doc, int chunkSize, int overlap)` → `List<Chunk>`
- [x] **Character-based** sliding window — no tokenizer (ADR-007)
- [x] Default: `chunk_size=512` chars, `overlap=64` chars
- [x] Title prepended to every chunk embed input: `"{title}. {body_slice}"` (ADR-003)
- [x] Each chunk carries `document_id` shared with source document
- [x] Chunk Qdrant point ID: `{document_id}-chunk-{N}`
- [x] `body_snippet` in payload: first 500 chars of chunk body, no title prefix (SPEC §14)
- [x] Config via `@Value`: `INGEST_DEFAULT_CHUNK_SIZE=512`, `INGEST_DEFAULT_OVERLAP=64`
- [x] `Chunk` Java record (code-quality.mdc B2)
- [x] No ML dependencies in `ChunkingService` (pure string operations)

### Acceptance criteria

- 2000-char document with chunk_size=512 / overlap=64 produces 5 chunks
- All chunks share the same `document_id`
- Title prefix appears in embed input for every chunk
- Body snippet has no title prefix, max 500 chars

### Test plan

- [x] Unit: chunk count for 2000-char body
- [x] Unit: overlap verified (chunk N+1 starts 448 chars after chunk N)
- [x] Unit: title prepending verified via `embedInput` field
- [x] Unit: body_snippet excludes title prefix
- [x] Unit: empty body → 0 chunks (or 1 empty chunk — document the choice)
- [x] Unit: body shorter than chunk_size → 1 chunk
- [x] Golden-file test: use article #1 from `fixtures/news_radar_dhaka_floods.json`

---

## Issue #7 — Ingest API (POST /api/ingest/documents)

**Labels:** `api`, `ingest`, `phase-2`
**Depends on:** #2, #6, ADR-008 (write first)
**Blocks:** #8, #9, #11

### Description

Core ingest orchestration: chunk → embed → upsert (SPEC §7.1–§7.2, §7.5, ADR-008).

### Tasks

- [x] `IngestController` — `POST /api/ingest/documents` with `@Valid @RequestBody`
- [x] `IngestService` — orchestrate ChunkingService → batched embed calls → Qdrant upsert
- [x] `IngestRequest` Java record: `collection`, `model`, `documents[]`, `chunking`, `upsert_mode`
- [x] `IngestResponse` Java record: `collection`, `ingested`, `chunks_created`, `skipped`, `errors`, `latency_ms`
- [x] Input validation: query max 1000 chars, collection name pattern, body max 100,000 chars (code-quality.mdc E2)
- [x] Upsert modes: `skip_existing`, `overwrite`, `error_on_conflict` (SPEC §7.5)
- [x] On `overwrite`: delete existing chunks by `document_id` payload filter before upserting
- [x] Batched embedding: `INGEST_BATCH_SIZE=32` env default
- [x] Qdrant payload per SPEC §14 + `document_id` per ADR-008
- [x] Qdrant filter conditions via typed client builder only — no string concatenation (code-quality.mdc E5)
- [x] `latency_ms` measured via `System.nanoTime()` (code-quality.mdc B9)
- [x] Embedding model mismatch → 400 with actionable error

### Acceptance criteria

- Ingest all 8 articles from `fixtures/news_radar_dhaka_floods.json` → correct chunk count
- `skip_existing` re-run → `skipped=8`, `ingested=0`
- `overwrite` re-run → replaces all chunks
- `error_on_conflict` on duplicate → 409 response
- Model mismatch → 400 with explanation
- Body > 100,000 chars → 400

### Test plan

- [x] Unit: `IngestService` with mocked embed client + mocked Qdrant
- [x] Unit: each upsert mode branching logic
- [x] Integration (Testcontainers): ingest 3 articles → correct Qdrant point count
- [x] Integration: `skip_existing` idempotency
- [x] Integration: `overwrite` replaces chunks
- [x] Contract test: response JSON matches SPEC §7.2 exactly
- [x] Security test: body > 100,000 chars → 400

---

## Issue #8 — Ingest API integration tests

**Labels:** `testing`, `api`, `ingest`, `phase-2`
**Depends on:** #7
**Blocks:** #9, #11

### Description

Full integration test suite for the ingest pipeline using
`fixtures/news_radar_dhaka_floods.json` as the test corpus.

### Tasks

- [x] Shared fixture: load `fixtures/news_radar_dhaka_floods.json` in `@BeforeAll`
- [x] Test: create collection → ingest all 8 articles → `GET /collections` shows correct point count
- [x] Test: chunking enabled vs disabled — point count differs
- [x] Test: News Radar shape (SPEC §7.6) — `id=cluster_id`, all payload fields present
- [x] Test: Bangla articles (language=bn) and English (language=en) both ingest correctly
- [x] Performance baseline: ingest 8 articles < 30s (excluding cold model load)
- [x] All tests use `fixtures/news_radar_dhaka_floods.json` — no ad-hoc inline data

### Acceptance criteria

- All ingest paths covered, reproducible in CI
- Idempotent re-ingest confirmed with `skip_existing`

### Test plan

- [x] `make test-int` runs these tests
- [x] Performance assertion present (< 30s)

---

## Issue #9 — Ingest CLI

**Labels:** `ingest-cli`, `phase-3`
**Depends on:** #7, #8
**Blocks:** #10, #11

### Description

Batch ingestion CLI per SPEC §7.3. Security rules apply: path traversal
prevention in file loader, SSRF prevention in URL loader (code-quality.mdc E3, E4).

### Tasks

- [x] `queriva_ingest.py` CLI entry point with `argparse` — all flags have `help=` strings
- [x] `json_loader.py` — loads array of document objects
- [x] `csv_loader.py` — tabular data with `--map` column mapping
- [x] `file_loader.py` — recursive `.txt`, `.md`, `.pdf`; path traversal check (code-quality.mdc E3)
- [x] `url_loader.py` — fetch + extract text; SSRF prevention (code-quality.mdc E4)
- [x] Stdin/JSONL support (`--format jsonl`)
- [x] `requirements.txt` pinned: `httpx`, `pypdf`, `trafilatura`
- [x] CLI exits non-zero with clear `stderr` on failure
- [x] No ML library imports — calls embed-sidecar via API only (code-quality.mdc E7)
- [x] `packages/ingest-cli/README.md` with CLI usage examples from SPEC §7.3

### Acceptance criteria

- `python queriva_ingest.py --format json --source ./fixtures/news_radar_dhaka_floods.json` ingests all 8 articles
- CSV column mapping works
- File loader picks up `.txt`, `.md`, `.pdf` recursively
- URL loader blocks private IP addresses (127.x.x.x, 10.x.x.x, 192.168.x.x)
- Path traversal attempt → error, not file read

### Test plan

- [x] Unit per loader with `tmp_path` fixture
- [x] Unit: URL loader blocks private IPs
- [x] Unit: file loader rejects path traversal
- [x] CLI black-box tests via `subprocess.run`
- [x] CLI exits non-zero on missing required args
- [x] Integration: CLI against running API with fixture file

---

## Issue #10 — Ingest CLI tests

**Labels:** `testing`, `ingest-cli`, `phase-3`
**Depends on:** #9
**Blocks:** #11

### Description

Full coverage before seed data depends on the CLI.

### Tasks

- [x] Parametrized tests across all loader formats (test-quality.mdc C3)
- [x] CLI argument parsing: missing args, invalid format, unknown flags
- [x] HTTP error handling: API down, 400, 409, 500
- [x] SSRF test: private IPs rejected
- [x] Path traversal test: `../../../etc/passwd` rejected
- [x] ≥ 80% coverage on loaders + CLI entry point

### Acceptance criteria

- All formats have passing tests
- Security edge cases covered
- `pytest --cov` ≥ 80%

### Test plan

- [x] `make test-ingest` passes
- [x] Tests run without network (mock httpx)

---

## Issue #11 — Seed demo data

**Labels:** `infra`, `ingest`, `qdrant`, `phase-3`
**Depends on:** #8, #9
**Blocks:** #14, #15

### Description

Use the fixture and seed script (both already exist from #0) to verify the
end-to-end ingest pipeline. This issue validates that `fixtures/news_radar_dhaka_floods.json`
and `scripts/seed-demo.sh` work correctly against a running stack.

### Tasks

- [x] Verify `fixtures/news_radar_dhaka_floods.json` — all 8 articles have all SPEC §14 payload fields
- [x] Verify `scripts/seed-demo.sh` runs end-to-end against docker compose stack
- [x] Add `make seed` Makefile target (if not already present)
- [x] Document seed step in README stub: "run `make seed` after `docker compose up`"
- [x] Verify both Bangla (BN) and English (EN) articles are present after seed

### Acceptance criteria

- `make seed` exits 0 against a running stack
- `GET /api/ingest/collections` shows `news_radar` with ≥ 8 points
- Seed is idempotent: running `make seed` twice produces no duplicates

### Test plan

- [x] `make seed` exits 0
- [x] `curl localhost:8080/api/ingest/collections` shows `news_radar` point count ≥ 8
- [x] Run `make seed` twice → same point count both times

---

## Issue #12 — QdrantSearchService

**Labels:** `api`, `qdrant`, `phase-4`
**Depends on:** #5
**Blocks:** #14

### Description

Vector search against Qdrant with typed filter builder (SPEC §8 step 3, ADR-001).
No string-concatenated filter DSL — typed builder only (code-quality.mdc E5).

### Tasks

- [x] `QdrantSearchService.search(collection, vector, topK, minScore, filters)` → `List<SearchHit>`
- [x] Typed Qdrant filter builder for: `language`, `date_from`, `date_to`, `category`
- [x] Filter results below `min_score` threshold
- [x] Group/deduplicate chunks by `document_id` — max one result per source document
- [x] `SearchHit` Java record mapping Qdrant payload → SPEC §6 response fields

### Acceptance criteria

- Search against seeded `news_radar` returns ranked results
- `language=bn` filter returns only Bangla articles
- Date range filter works on `published_at`
- Multiple chunks from same document → only one result in top-k

### Test plan

- [x] Unit: mocked Qdrant client, verify filter builder called correctly
- [x] Integration (Testcontainers + seeded data from fixture): language filter
- [x] Integration: date range filter
- [x] Integration: `min_score=0.99` → empty results
- [x] Integration: chunk deduplication — only one result per `document_id`
- [x] Parameterized test for filter combinations (test-quality.mdc B9)

---

## Issue #13 — QueryEmbeddingService

**Labels:** `api`, `embed-sidecar`, `phase-4`
**Depends on:** #5, #2
**Blocks:** #14

### Description

HTTP client from Spring Boot to embed-sidecar for query vectorisation (SPEC §8 step 2).

### Tasks

- [x] `QueryEmbeddingService.embed(text, model)` → `float[]`
- [x] Configurable via `EMBED_DEFAULT_MODEL` env
- [x] Connect timeout: 5s, read timeout: 30s
- [x] One retry on 5xx or IOException — no retry on 4xx (code-quality.mdc B5)
- [x] Model mismatch stored in collection metadata — validate before search, return 400 if mismatch
- [x] WireMock for unit tests (test-quality.mdc B6)

### Acceptance criteria

- Returns 768-dim float array for LaBSE
- Throws `EmbedSidecarException` with actionable message when sidecar down
- Retry fires once on 503, does not retry on 400

### Test plan

- [x] Unit (WireMock): 200 → correct vector returned
- [x] Unit (WireMock): 503 → one retry → success
- [x] Unit (WireMock): 503 → one retry → 503 → exception thrown with message
- [x] Unit (WireMock): 400 → no retry, exception immediately
- [x] Unit: timeout fires after 30s (use WireMock delay)
- [x] Integration: real embed sidecar, LaBSE model

---

## Issue #14 — POST /api/search (search mode)

**Labels:** `api`, `phase-4`
**Depends on:** #11, #12, #13
**Blocks:** #15, #17

### Description

Full search-mode flow: embed → search → map response (SPEC §8, §6 contract).

### Tasks

- [x] `SearchController` — `POST /api/search` in `dev.queriva.search`
- [x] `SearchRequest` record: `query` (@NotBlank, @Size max=1000), `collection` (pattern), `topK`, `minScore`, `mode`, `filters`
- [x] Request defaults in compact constructor: `topK=10`, `minScore=0.60`, `mode="search"`
- [x] `SearchResultMapper` — map `List<SearchHit>` to `SearchResponse` per SPEC §6
- [x] `latency_ms` breakdown: `embed`, `search`, `total` in search mode; `synthesis=null`
- [x] `summary=null` in search mode
- [x] Query logged at DEBUG level only (code-quality.mdc E1)

### Acceptance criteria

- `"floods in Dhaka last week"` against seeded `news_radar` returns ≥ 4 results
- Response JSON matches SPEC §6 exactly (null fields absent or null)
- Invalid collection → 404
- Query > 1000 chars → 400
- `latency_ms.synthesis` absent in search mode response

### Test plan

- [x] `@WebMvcTest`: blank query → 400
- [x] `@WebMvcTest`: query > 1000 chars → 400
- [x] `@WebMvcTest`: invalid collection name → 400
- [x] Integration: "floods in Dhaka" → ≥ 4 results with score > 0.60
- [x] Integration: all filter combinations (parametrized)
- [x] Contract test: response JSON schema matches SPEC §6
- [x] Latency test: `latency_ms` fields populated and positive

---

## Issue #15 — Search mode integration tests

**Labels:** `testing`, `api`, `phase-4`
**Depends on:** #14
**Blocks:** #17

### Description

Full integration coverage for search mode before RAG complexity is added.

### Tasks

- [x] Fixture loaded from `fixtures/news_radar_dhaka_floods.json` in `@BeforeAll`
- [x] Test: expected top result title matches article #1 from fixture
- [x] Test: `top_k=3` returns exactly 3 results
- [x] Test: `min_score=0.99` returns 0 results
- [x] Test: `language=bn` returns only Bangla articles
- [x] Test: `language=en` returns only English articles
- [x] Test: date range filter narrows results
- [x] Performance baseline: search mode < 500ms (code-quality.mdc E5 equivalent)
- [x] Full response shape asserted (test-quality.mdc E2)

### Acceptance criteria

- All paths covered and reproducible in CI
- Performance baseline asserted in test

### Test plan

- [x] `make test-int` includes these tests
- [x] No ad-hoc fixture data — all from `fixtures/news_radar_dhaka_floods.json`

---

## Issue #16 — Ollama Docker setup

**Labels:** `infra`, `ollama`, `phase-5`
**Depends on:** #14
**Blocks:** #17

### Description

Ollama service in Docker Compose with Mistral 7B (SPEC §12, ADR-006).

### Tasks

- [x] `ollama` service in `docker-compose.yml` — port 11434, volume, healthcheck
- [x] Model pull documented: `ollama pull mistral` (or init script on first start)
- [x] `OLLAMA_URL`, `OLLAMA_MODEL` in `.env.example`
- [x] `GET /api/health` shows `ollama: connected` when service is up

### Acceptance criteria

- `docker compose up ollama` → `curl localhost:11434/api/tags` returns model list
- Health endpoint reflects Ollama status

### Test plan

- [x] Manual: `curl localhost:11434/api/generate` with sample prompt → response
- [x] Health integration test: Ollama up → health shows connected; down → shows disconnected

---

## Issue #17 — RAG mode (LLMSynthesisService)

**Labels:** `api`, `ollama`, `phase-5`
**Depends on:** #15, #16
**Blocks:** #18

### Description

LLM synthesis from retrieved chunks via Ollama (SPEC §8 step 4b, §10 prompt, ADR-006).

### Tasks

- [x] `LLMSynthesisService.synthesize(query, hits)` → `String summary`
- [x] Prompt template per SPEC §10: system + numbered articles + user question
- [x] `mode=rag` → `summary` non-null + `latency_ms.synthesis` populated
- [x] `SEARCH_MAX_SCORE_AUTO_ACCEPT=0.80` — skip LLM if top result score ≥ threshold
- [x] Graceful degradation: Ollama down → return search results without summary, `summary=null`
- [x] Query NOT logged at INFO level — DEBUG only (code-quality.mdc E1)
- [x] WireMock Ollama stub for unit tests

### Acceptance criteria

- RAG query returns non-empty `summary` citing article titles
- `latency_ms.synthesis` > 0 in RAG mode
- `SEARCH_MAX_SCORE_AUTO_ACCEPT` correctly skips LLM
- Ollama unreachable → search results returned, `summary=null`, no exception

### Test plan

- [x] Unit: prompt builder formats numbered articles correctly
- [x] Unit (WireMock): Ollama 200 → summary returned
- [x] Unit (WireMock): Ollama 503 → graceful degradation
- [x] Integration (`@Tag("slow")`): real Ollama, "floods in Dhaka" → non-empty summary
- [x] Test: search mode vs RAG mode same query → different response shapes

---

## Issue #18 — RAG mode integration tests

**Labels:** `testing`, `api`, `phase-5`
**Depends on:** #17
**Blocks:** #19

### Description

End-to-end RAG validation before UI is built on top of it.

### Tasks

- [x] Full stack: query → embed → search → synthesize against docker compose
- [x] Assert `summary` non-empty for "floods in Dhaka last week"
- [x] Assert `latency_ms.synthesis` > 0
- [x] Assert `latency_ms.total` > `latency_ms.synthesis`
- [x] Same query: `mode=search` → no summary; `mode=rag` → has summary
- [x] Record baseline latency range in test comment (~2–5s per SPEC §15)

### Acceptance criteria

- RAG integration tests pass against docker compose stack
- Baseline latencies documented

### Test plan

- [x] `make test-int` or manual against `docker compose up`
- [x] Test tagged `@Tag("slow")` to separate from fast CI

---

## Issue #19 — UI scaffold + design tokens + brand assets

**Labels:** `ui`, `design`, `phase-6`
**Depends on:** #0, #1
**Blocks:** #20, #21, #22

### Description

React/Vite package setup with design system and brand assets from `mock-ui/`.

### Tasks

- [x] Copy brand assets from `mock-ui/queriva-icon-files/` → `packages/ui/public/`
- [x] `favicon.svg` in `packages/ui/public/`
- [x] CSS variables matching mock UI: `--qv-navy: #0D1B2A`, `--qv-teal: #00C9B8`, `--qv-amber: #F6AD55`
- [x] Light + dark theme support
- [x] Tabler Icons installed
- [x] `src/types/api.ts` — TypeScript types derived from SPEC §6 (code-quality.mdc D2)
- [x] `src/constants/ui.ts` — all UI strings (code-quality.mdc D7)
- [x] `tsconfig.json`: `"strict": true` (code-quality.mdc D1)
- [x] All package versions pinned exactly in `package.json` (code-quality.mdc E6)

### Acceptance criteria

- Brand assets present and correct
- CSS variables match mock UI exactly
- TypeScript strict mode enabled
- All API types defined from SPEC §6

### Test plan

- [x] Type compilation: `tsc --noEmit` exits 0
- [x] Visual check: CSS variables render correctly in browser

---

## Issue #20 — Search zone components

**Labels:** `ui`, `phase-6`
**Depends on:** #19
**Blocks:** #23

### Description

Header bar, search input, mode toggle, filter chips (mock UI `.qv-bar`, `.qv-search-zone`).

### Components

- [x] `TopBar` — logo, brand, status pill from `GET /api/health`, icons with `aria-label`
- [x] `SearchBar` — input with `aria-label`, submit on Enter or button click
- [x] `ModeToggle` — segmented Search / RAG, keyboard accessible
- [x] `FilterStrip` — language, date range, category, collection badge chips
- [x] `ResultCount` — "N results" text

### Acceptance criteria

- Layout matches mock UI at 1280px
- Status pill shows real health state
- Every interactive element has `aria-label` or `aria-hidden`

### Test plan

- [x] RTL: `ModeToggle` click changes mode state
- [x] RTL: `FilterStrip` chip click emits filter change
- [x] RTL: `TopBar` status pill renders health state from MSW
- [x] axe-core: no critical violations (test-quality.mdc D6)
- [x] Loading state: status pill shows "checking..." before health response

---

## Issue #21 — Results list + stats components

**Labels:** `ui`, `phase-6`
**Depends on:** #19
**Blocks:** #23

### Description

Left column results and right column stats panel.

### Components

- [ ] `ResultCard` — rank, title, score bar (dynamic width%), snippet, source, language badge, date
- [ ] `ResultsList` — "Matching articles" label, top-result highlight border
- [ ] `LoadMoreButton` — increments `top_k`
- [ ] `StatsPanel` — hits, best score, languages, total time metric cards

### Acceptance criteria

- Score bar width = `score * 100%` (dynamic inline style — the one exception to no-inline-style rule)
- BN badge: teal-50 bg + teal-800 text. EN badge: blue-50 bg + blue-800 text
- Card `onClick` fires `onResultClick` prop in widget mode

### Test plan

- [ ] RTL: `ResultCard` renders all fields from mock API response
- [ ] RTL: score bar has correct width style
- [ ] RTL: empty results → `EmptyState` component shown
- [ ] RTL: loading → skeleton shown

---

## Issue #22 — AI summary panel

**Labels:** `ui`, `phase-6`
**Depends on:** #19
**Blocks:** #23

### Description

RAG summary card, latency footer, copy/refresh actions, suggestions panel.

### Components

- [ ] `AISummary` — header with dot + label, body with inline reference badges `[N]`
- [ ] `LatencyFooter` — embed / search / llm breakdown
- [ ] Copy button (`aria-label="Copy summary"`) — copies summary to clipboard
- [ ] Refresh button (`aria-label="Refresh"`) — re-runs same query
- [ ] `SuggestionsPanel` — three follow-up query buttons

### Acceptance criteria

- Hidden completely in search mode
- Reference badges `[1]` styled per mock
- Copy to clipboard works
- Panel not visible until `summary` is non-null

### Test plan

- [ ] RTL: panel hidden when `mode=search`
- [ ] RTL: panel visible when `mode=rag` and `summary` present
- [ ] RTL: copy button fires clipboard write
- [ ] RTL: refresh button re-triggers `useSearch`

---

## Issue #23 — Standalone app + useSearch hook

**Labels:** `ui`, `phase-6`
**Depends on:** #18, #20, #21, #22
**Blocks:** #24, #25

### Description

Wire all UI components into standalone SPA with data fetching hook (SPEC §5).

### Tasks

- [ ] `useSearch` hook with `useReducer` (code-quality.mdc D6): states = idle, loading, success, error
- [ ] `POST /api/search` integration
- [ ] Two-column layout per mock UI
- [ ] `App.tsx` at port 3000
- [ ] Env: `VITE_API_URL`, `VITE_DEFAULT_COLLECTION`
- [ ] Explicit loading, error, empty states (code-quality.mdc D9)

### Acceptance criteria

- "floods in Dhaka last week" → results + RAG summary end to end
- Mode toggle and filters wired through `useSearch`
- Loading skeleton shown during request
- Error banner shown on API failure

### Test plan

- [ ] RTL + MSW: full search flow (search mode)
- [ ] RTL + MSW: RAG mode shows AI summary
- [ ] RTL + MSW: loading state shown during pending request
- [ ] RTL + MSW: error state shown on 500
- [ ] Manual: against docker compose backend

---

## Issue #24 — Standalone UI tests

**Labels:** `testing`, `ui`, `phase-6`
**Depends on:** #23
**Blocks:** #25

### Description

Full UI test coverage before Module Federation.

### Tasks

- [ ] MSW handlers for `/api/search` (both modes) and `/api/health`
- [ ] Test: search flow end-to-end with MSW
- [ ] Test: RAG toggle — summary appears/disappears
- [ ] Test: filter chips wired to request parameters
- [ ] Test: `useSearch` hook via `renderHook` (test-quality.mdc D5)
- [ ] axe-core audit on `App` component
- [ ] ≥ 80% coverage on components and hooks

### Acceptance criteria

- `turbo run test --filter=ui` exits 0
- No critical axe violations
- ≥ 80% coverage

### Test plan

- [ ] `make test-ui` passes with coverage report
- [ ] axe audit passes

---

## Issue #25 — Module Federation widget (SearchWidget)

**Labels:** `ui`, `mfe`, `phase-7`
**Depends on:** #24, ADR-010 (write first)
**Blocks:** #26, #27

### Description

Export embeddable `SearchWidget` via Vite Module Federation (SPEC §11, ADR-010).

### Tasks

- [ ] Vite Module Federation config: `name: 'queriva'`, `exposes: {'./SearchWidget': './src/SearchWidget.tsx'}`
- [ ] `SearchWidget.tsx` with all props from SPEC §11 `SearchWidgetProps` interface
- [ ] Widget functional with only `apiUrl` + `collection` — all others optional with defaults (code-quality.mdc D10)
- [ ] `react` and `react-dom` shared between host and remote
- [ ] `SearchWidgetProps` TypeScript interface exported alongside component
- [ ] Example host app at `packages/ui/examples/host/`
- [ ] `remoteEntry.js` URL configurable via env var in host

### Acceptance criteria

- Host loads widget from `http://localhost:5173/assets/remoteEntry.js`
- All `SearchWidgetProps` override defaults correctly
- `onResultClick` fires with correct `SearchResult` argument
- `theme` prop applies correct CSS class

### Test plan

- [ ] Build remote + host: both `turbo run build` without errors
- [ ] Manual: host app renders `SearchWidget` and search works
- [ ] Test: `SearchWidgetProps` contract — all props tested (test-quality.mdc D7)

---

## Issue #26 — MFE integration tests

**Labels:** `testing`, `mfe`, `phase-7`
**Depends on:** #25
**Blocks:** #27

### Description

Verify MFE export stability and TypeScript props contract.

### Tasks

- [ ] `SearchWidget` isolation tests — all props
- [ ] Shared components between `App.tsx` and `SearchWidget.tsx` verified
- [ ] TypeScript types exported: `SearchWidgetProps`, `SearchResult`, `SearchMode`
- [ ] MFE integration guide verified: code sample from SPEC §11 compiles

### Acceptance criteria

- Both entry points (`App.tsx` and `SearchWidget.tsx`) build and work
- TypeScript types exportable by host app without errors

### Test plan

- [ ] CI builds federation bundle without errors
- [ ] Host app example compiles with exported types

---

## Issue #27 — Full Docker Compose stack

**Labels:** `infra`, `docker`, `phase-8`
**Depends on:** #18, #25
**Blocks:** #28

### Description

One-command full stack per SPEC §12. All services with healthchecks and
proper dependency ordering.

### Tasks

- [ ] `docker-compose.yml` — all 5 services: qdrant, ollama, embed-sidecar, api, ui
- [ ] Healthchecks on all services
- [ ] `depends_on` with `condition: service_healthy` for api
- [ ] `docker-compose.dev.yml` — hot reload for api and ui
- [ ] `.env.example` complete with all config keys from SPEC §13
- [ ] `make seed` documented as post-start step
- [ ] CORS env var `CORS_ALLOWED_ORIGINS` included in `.env.example`

### Acceptance criteria

- `docker compose up` → `localhost:3000` search works
- `docker compose down -v` → `docker compose up` → still works
- Ingest CLI can reach `localhost:8080` from host machine

### Test plan

- [ ] Fresh clone → `docker compose up` → `make seed` → search "floods in Dhaka" → results appear
- [ ] `docker compose down -v` + retry
- [ ] Dev compose: change UI file → browser reloads

---

## Issue #28 — End-to-end smoke tests

**Labels:** `testing`, `e2e`, `phase-8`
**Depends on:** #27
**Blocks:** #29

### Description

Full stack E2E validation: ingest → search → RAG. Two layers: Playwright + curl script.

### Tasks

- [ ] `scripts/e2e-smoke.sh` — curl-only smoke test (already scaffolded in #0, verify it passes)
- [ ] Playwright test: open UI → health pills green → search "floods in Dhaka" → ≥ 4 results
- [ ] Playwright test: toggle RAG mode → AI summary panel appears
- [ ] Playwright test: click result card → URL opens
- [ ] Playwright tests are deterministic — run against pre-seeded stack (no in-test seeding)
- [ ] `make smoke` Makefile target runs Playwright tests

### Acceptance criteria

- `scripts/e2e-smoke.sh` exits 0 against running stack
- `make smoke` exits 0
- Both ingest and search paths exercised

### Test plan

- [ ] `scripts/e2e-smoke.sh` manual run
- [ ] `make smoke` against `docker compose up` + `make seed`

---

## Issue #29 — README + documentation

**Labels:** `docs`, `phase-9`
**Depends on:** #28
**Blocks:** #30

### Description

Final documentation per SPEC §18 milestone 11. Follow `README-TEMPLATE.md` exactly.

### Tasks

- [ ] `README.md` — use `README-TEMPLATE.md` as the exact template (queriva.mdc Rule 20)
- [ ] Validate: fresh clone → `docker compose up` → `make seed` → search works in < 20 minutes
- [ ] Architecture diagram matches SPEC §4
- [ ] Ingestion guide: API + CLI + News Radar integration (SPEC §7.6)
- [ ] Configuration reference: all env vars from SPEC §13
- [ ] Qdrant payload schema: SPEC §14 including `document_id`
- [ ] MFE integration guide: code sample from SPEC §11
- [ ] Development guide: run packages individually, tests, seed
- [ ] Troubleshooting: model mismatch, Qdrant connection, Ollama model pull, first-start slowness
- [ ] No placeholder text or "TODO" in committed README
- [ ] CHANGELOG.md `[Unreleased]` entries moved to `## [1.0.0]`

### Acceptance criteria

- Fresh clone to working search in < 20 minutes following README
- Architecture diagram accurate
- All troubleshooting scenarios documented

### Test plan

- [ ] Self-review: follow README from scratch on a clean machine
- [ ] `README-TEMPLATE.md` sections all present in `README.md`

---

## Issue #30 — Release v1.0.0

**Labels:** `release`, `docs`, `phase-9`
**Depends on:** #29
**Blocks:** —

### Description

Cut the v1.0.0 release per SPEC §18 and `queriva.mdc` Rule 17.

### Tasks

- [ ] Move all `[Unreleased]` CHANGELOG entries to `## [1.0.0] — YYYY-MM-DD`
- [ ] Add one-line tagline: `> Client-presentable: fresh clone to working demo in under 20 minutes`
- [ ] Verify: `make test` exits 0
- [ ] Verify: `make smoke` exits 0
- [ ] Verify: CI badge green on `main`
- [ ] Verify: fresh clone → working in < 20 minutes
- [ ] Commit: `release: v1.0.0`
- [ ] Tag: `git tag -a v1.0.0 -m "Release v1.0.0"`
- [ ] GitHub release: paste CHANGELOG `[1.0.0]` section as release notes
- [ ] Record Loom demo: full product walkthrough for Upwork proposals
- [ ] Update Upwork profile with Queriva link, UI screenshot, Loom link
- [ ] Write blog post: "Building Queriva — local RAG search, no external APIs"

### Acceptance criteria

- GitHub release `v1.0.0` exists with release notes
- Loom demo recorded and linked in GitHub release
- Upwork profile updated

### Test plan

- [ ] `make test && make smoke` exits 0 on tagged commit
- [ ] GitHub release visible and notes accurate

---

## Out of scope (v1)

Per SPEC §3:
- Authentication / multi-tenancy
- Cloud deployment
- Streaming ingestion from Kafka / Redpanda (planned v2)
- Non-Qdrant vector stores
- Settings panel (icon in mock is stub only)
- Menu navigation (icon in mock is stub only)

---

## GitHub labels

| Label | Color | Use |
|---|---|---|
| `adr` | `#d4c5f9` | ADR writing issues |
| `phase-0` | `#ededed` | Foundation |
| `phase-1` | `#c2e0c6` | Embed sidecar |
| `phase-2` | `#bfd4f2` | Collection mgmt + ingest API |
| `phase-3` | `#d4c5f9` | Ingest CLI + seed |
| `phase-4` | `#f9d0c4` | Search API |
| `phase-5` | `#e8c4f9` | RAG |
| `phase-6` | `#f5e6a3` | UI |
| `phase-7` | `#bfe5e5` | MFE |
| `phase-8` | `#c2e0c6` | Docker + E2E |
| `phase-9` | `#ffffff` | Docs + release |
| `api` | | Spring Boot |
| `ui` | | React frontend |
| `ingest` | | Ingest API |
| `ingest-cli` | | Python CLI |
| `embed-sidecar` | | FastAPI |
| `infra` | | Docker, CI |
| `ci` | | GitHub Actions |
| `testing` | | Test-only |
| `design` | | Visual/branding |
| `release` | | Release issues |
| `docs` | | Documentation |
| `security` | | Security-related tasks |

---

## Estimated effort (sole developer)

| Phase | Issues | Focus | Rough effort |
|---|---|---|---|
| meta — ADRs | #meta | All 12 ADRs | 1–2 days |
| 0 — Foundation | #0–#1b | Monorepo, foundation files, CI | 2–3 days |
| 1 — Embed | #2–#3 | Embed sidecar | 1–2 days |
| 2 — Ingest API | #4–#8 | Qdrant, collections, chunking, ingest | 4–5 days |
| 3 — Ingest CLI | #9–#11 | CLI loaders + seed validation | 2–3 days |
| 4 — Search API | #12–#15 | Search mode | 3–4 days |
| 5 — RAG | #16–#18 | Ollama synthesis | 2–3 days |
| 6 — UI | #19–#24 | Standalone SPA | 4–5 days |
| 7 — MFE | #25–#26 | Module Federation | 1–2 days |
| 8 — Docker + E2E | #27–#28 | Full stack | 2 days |
| 9 — Docs + release | #29–#30 | README + v1.0.0 | 2–3 days |
| **Total** | **32 issues** | | **~24–32 days** |

---

## Mapping to SPEC §17 milestones and §18 versions

| SPEC milestone | Issues | Version |
|---|---|---|
| ADRs | #meta | — |
| 1 — Collection management | #5 | v0.1.0 |
| 6 — Embed sidecar | #2, #3 | v0.1.0 |
| 4 — Chunking | #6 | v0.2.0 |
| 2 — Core ingest API | #7, #8 | v0.2.0 |
| 3 — Ingest CLI | #9, #10 | v0.2.0 |
| 5 — Core search API | #14, #15 | v0.3.0 |
| 7 — RAG mode | #17, #18 | v0.3.0 |
| 8 — Standalone UI | #19–#24 | v0.4.0 |
| 9 — MFE widget | #25, #26 | v0.5.0 |
| 10 — Docker Compose | #27 | v0.5.0 |
| 11 — README + Docs | #29 | v1.0.0 |
| — Release | #30 | v1.0.0 |
