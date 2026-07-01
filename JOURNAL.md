# Queriva — Development Journal

Personal log of daily progress, blockers, and decisions.
One entry per working day. Most recent entry at the top.

---

## 2026-06-30

### Built
- **Issue #5** — API skeleton + health + collection management
  - `GET /api/health` — probes Qdrant (gRPC), Ollama, embed-sidecar; returns `ok` or `degraded`
  - `POST /api/ingest/collection`, `GET /api/ingest/collections`, `DELETE /api/ingest/collection/{name}`
  - `CollectionManager` via `io.qdrant:client` 1.14.1; `GlobalExceptionHandler`, CORS, config beans
  - `@WebMvcTest` unit tests (health + collection endpoints); Testcontainers ITs for create/list/delete
  - `packages/api/Dockerfile` with curl healthcheck
- **Issue #2** — embed sidecar (`issue-2/embed-sidecar`, PR #35)
  - `POST /api/embed` and `GET /api/health` per SPEC §6 and §9
  - `model_loader.py` — lazy cache for LaBSE (768), MiniLM (384), multilingual-mpnet (768)
  - `config.py`, `Dockerfile`, pinned sentence-transformers + torch
  - Unit tests with mocked `SentenceTransformer` — 12 passed, 96% coverage
- **Issue #3** — embed sidecar full test coverage (`issue-3/embed-sidecar-test-coverage`)
  - Concurrent embed requests — model loaded only once (thread-safe loader)
  - Parametrized invalid model name → 422; dimension mismatch → 500
  - Health endpoint SPEC contract test; `model_loader` unit tests
  - Coverage floor raised to 90% in `pytest.ini`
- **Issue #4** — Qdrant Docker service (`issue-4/qdrant-docker-service`)
  - `docker-compose.yml` with qdrant on 6333/6334, `qdrant_data` volume, curl healthcheck
  - `infra/docker/qdrant.Dockerfile` — curl added to official image for `/healthz` probe
  - `QdrantTestcontainersSupport` + expanded `QdrantContainerIT` (healthz, collections)

### Blocked
- Nothing

### Decided
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
- Issue #6 — ChunkingService
- Issue #7 — Ingest API
- Issue #8 — Ingest API integration tests

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
