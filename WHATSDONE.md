# What's Done — Issues #0 → #1b

Chronological record of every file added or changed from the monorepo scaffold through
the GitHub Actions CI pipeline. Each entry explains **what** changed and **why**.

> **Scope:** Issues #0, #1, #1b only. Foundation files (SPEC, ADRs, Cursor rules,
> fixture, seed script) were committed in the `[meta]` commit before #0 and are not
> repeated here unless #0 touched them.

---

## Issue #0 — Scaffold Turborepo monorepo

**Goal:** Bootstrap the four-package monorepo per SPEC §5. Structure and tooling only —
no business logic.

### Root — orchestration and repo hygiene

| File | Why |
|---|---|
| `package.json` | npm workspaces over `packages/*`; declares Turborepo 2.3.3 and root scripts (`build`, `test`) so one command runs all packages |
| `package-lock.json` | Pins exact Node dependency tree for reproducible installs (Rule E6) |
| `turbo.json` | Defines build/test pipeline with dependency order (`build` before `test`) and output caches per ADR-012 |
| `Makefile` | Single command interface (Rule 11): `install`, `build`, `test`, `smoke`, `seed`, `clean`, `help` |
| `.env.example` | All SPEC §13 config keys documented at repo root — ingest, Qdrant, Ollama, embed sidecar, API, search, UI |
| `.gitignore` | Added `.vscode/` to existing ignore rules (Rule 19 — only `.cursor/` is committed) |
| `CHANGELOG.md` | `[Unreleased]` entries for #0 infrastructure and package scaffolds |
| `JOURNAL.md` | Daily log entry documenting #0 decisions and verification |

### `packages/api/` — Spring Boot 3.4 / Java 21

| File | Why |
|---|---|
| `pom.xml` | Maven project on Spring Boot 3.4.2, Java 21 — web, validation, actuator starters only at this stage |
| `package.json` | Turborepo shim so `turbo run build` can call `mvn package` (ADR-012 polyglot pattern) |
| `QuerivaApplication.java` | Spring Boot entry point |
| `application.yml` | `API_PORT` default and actuator health exposure |
| `search/SearchController.java` | Stub — HTTP search endpoints come in #14 |
| `search/QueryEmbeddingService.java` | Stub — embed-sidecar client in #13 |
| `search/QdrantSearchService.java` | Stub — vector search in #12 |
| `search/LLMSynthesisService.java` | Stub — Ollama RAG in #17 |
| `search/SearchResultMapper.java` | Stub — DTO mapping in #14 |
| `ingest/IngestController.java` | Stub — ingest HTTP endpoints in #7 |
| `ingest/IngestService.java` | Stub — chunk→embed→upsert orchestration in #7 |
| `ingest/ChunkingService.java` | Stub — sliding-window chunking in #6 |
| `ingest/CollectionManager.java` | Stub — Qdrant collection lifecycle in #5 |

### `packages/embed-sidecar/` — FastAPI skeleton

| File | Why |
|---|---|
| `main.py` | FastAPI app with `GET /api/health` stub — proves the sidecar process starts |
| `models.py` | Pydantic shapes for `EmbedRequest`, `EmbedResponse`, `HealthResponse` (Rule C2) |
| `requirements.txt` | Pinned runtime deps: fastapi, uvicorn, pydantic — no ML libs yet (Rule E7) |
| `package.json` | Turborepo shim; `build` runs `python3 -m compileall` |

### `packages/ingest-cli/` — Python CLI skeleton

| File | Why |
|---|---|
| `queriva_ingest.py` | CLI entry with argparse (`--collection`, `--format`, `--source`, `--api-url`) |
| `chunker.py` | Stub — character-based chunker implemented in #6/#9 |
| `loaders/json_loader.py` | Stub — JSON ingest in #9 |
| `loaders/csv_loader.py` | Stub — CSV ingest in #9 |
| `loaders/file_loader.py` | Stub — txt/md/pdf ingest in #9 |
| `loaders/url_loader.py` | Stub — URL ingest in #9 |
| `loaders/__init__.py` | Makes `loaders/` a Python package |
| `requirements.txt` | Pinned `httpx` for future API calls from CLI |
| `README.md` | Usage example pointing to issue #9 |
| `package.json` | Turborepo shim |

### `packages/ui/` — React 18 + Vite + TypeScript

| File | Why |
|---|---|
| `package.json` | Pinned React 18.3.1, Vite 6, TypeScript 5.7 — no `^` ranges (Rule E6) |
| `index.html` | Vite SPA shell |
| `vite.config.ts` | Dev server on port 5173 (SPEC UI default) |
| `tsconfig.json` / `tsconfig.app.json` / `tsconfig.node.json` | Strict TypeScript project references (Rule D1) |
| `src/main.tsx` | React root mount |
| `src/App.tsx` | Placeholder standalone SPA — "implementation in progress" |
| `src/App.css`, `src/index.css` | Minimal styling for scaffold |
| `src/vite-env.d.ts` | Vite type references |
| `src/SearchWidget.tsx` | Stub — MFE export in #25 |
| `src/components/SearchBar.tsx` | Stub — search UI in #20 |
| `src/components/ResultCard.tsx` | Stub — results UI in #21 |
| `src/components/AISummary.tsx` | Stub — RAG panel in #22 |
| `src/components/ModeToggle.tsx` | Stub — search/RAG toggle in #20 |
| `src/hooks/useSearch.ts` | Stub — data-fetching hook in #20 |

**Acceptance:** `turbo run build` exits 0 across all four packages.

---

## Issue #1 — Test infrastructure

**Goal:** Testing conventions and tooling in every package so all later issues can
add real tests without re-scaffolding. See ADR-009.

### Root

| File | Why |
|---|---|
| `Makefile` | `install-python` now installs `requirements-dev.txt` for both Python packages; uses `python3 -m pip` (macOS has no `python` shim); removed `|| true` fallbacks from per-package test targets |
| `CHANGELOG.md` | `[Unreleased]` entries for test infra, tag conventions, coverage |
| `JOURNAL.md` | #1 entry — blockers, decisions, verification |

### `packages/api/` — JUnit 5 + Testcontainers + WireMock

| File | Why |
|---|---|
| `pom.xml` | Added AssertJ, Testcontainers 1.20.4, WireMock 3.10 (`wiremock-standalone`); Maven Surefire tag filtering; `-Punit` / `-Pintegration` profiles per ADR-009 |
| `package.json` | `test-unit` → `mvn test -Punit`; `test-int` → `mvn test -Pintegration` |
| `TESTING.md` | Documents `@Tag("unit")`, `@Tag("integration")`, `@Tag("slow")` and which Makefile target runs each |
| `QuerivaApplicationTest.java` | `@Tag("unit")` — verifies Spring context loads |
| `WireMockSetupTest.java` | `@Tag("unit")` — proves WireMock stubs HTTP for future embed/Ollama client tests |
| `QdrantContainerIT.java` | `@Tag("integration")` — Testcontainers starts `qdrant/qdrant:latest`, hits REST port; validates Docker + Qdrant before #5/#12 |

### `packages/embed-sidecar/` — pytest + httpx + respx

| File | Why |
|---|---|
| `requirements-dev.txt` | Pinned pytest, pytest-cov, pytest-asyncio, httpx, respx |
| `pytest.ini` | 80% coverage floor, `@pytest.mark.unit` / `integration` markers, asyncio config |
| `TESTING.md` | Tag conventions and run commands |
| `package.json` | Real `test` / `test-unit` / `test-int` scripts; integration runs with `--no-cov` |
| `tests/test_health.py` | `@pytest.mark.unit` — async httpx client against FastAPI health endpoint |
| `tests/test_respx.py` | `@pytest.mark.unit` — demonstrates respx HTTP mocking pattern for future tests |
| `tests/test_integration_placeholder.py` | `@pytest.mark.integration` — placeholder until #3 |

### `packages/ingest-cli/` — pytest + subprocess tests

| File | Why |
|---|---|
| `requirements-dev.txt` | Pinned pytest, pytest-cov, pytest-httpx |
| `pytest.ini` | 80% coverage floor and marker conventions |
| `TESTING.md` | Tag conventions; documents subprocess black-box CLI testing (Rule C6) |
| `package.json` | Real test scripts with marker filtering |
| `tests/test_cli.py` | Subprocess tests — missing args exit non-zero; valid args exit 0 |
| `tests/test_main.py` | Direct `main()` unit tests for coverage of `queriva_ingest.py` |
| `tests/test_loaders.py` | Verifies each loader stub raises `NotImplementedError` until #9 |
| `tests/test_chunker.py` | Verifies chunker stub raises until #6 |
| `tests/test_integration_placeholder.py` | `@pytest.mark.integration` — placeholder until #10 |

### `packages/ui/` — Vitest + RTL + MSW + jest-axe

| File | Why |
|---|---|
| `package.json` | Added @testing-library/react, jest-dom, user-event, jsdom, msw, jest-axe, @vitest/coverage-v8 |
| `vitest.config.ts` | Separate from Vite build config; jsdom environment, coverage thresholds (80% lines) |
| `vite.config.ts` | Stripped `test` block — build-only config so `tsc -b` stays clean |
| `tsconfig.app.json` | Excludes `*.test.tsx` and test helpers from production type-check |
| `TESTING.md` | MSW mocking rule, jest-axe requirement, coverage scope |
| `src/setupTests.ts` | MSW server lifecycle (listen/reset/close) for all component tests |
| `src/__tests__/handlers.ts` | MSW handlers for `/api/health` — pattern for future search API mocks |
| `src/App.test.tsx` | Render test + jest-axe accessibility audit on `App` |
| `src/jest-axe.d.ts` | TypeScript declarations for jest-axe matchers in Vitest |

**Acceptance:** `make test-unit` (no Docker), `make test-int` (Docker + Testcontainers), and `make test` all exit 0.

---

## Issue #1b — GitHub Actions CI

**Goal:** CI on every PR and push to `main`, using Makefile targets exclusively.

| File | Why |
|---|---|
| `.github/workflows/ci.yml` | **New.** Triggers on `push`/`pull_request` to `main`; sets up Node 20, Java 21, Python 3.11 on `ubuntu-latest`; caches npm, Maven, pip; runs `docker info` then `make install` → `make build` → `make test`; concurrency group cancels stale runs |
| `CONTRIBUTING.md` | **Updated.** CI pipeline description and manual branch-protection setup instructions (GitHub Settings — not automatable in a workflow file) |
| `CHANGELOG.md` | `[Unreleased]` entries for CI workflow, caches, badge |
| `JOURNAL.md` | #1b entry — decisions on single-job matrix and `make install` vs `npm ci` |

**Already present (foundation, not changed in #1b):**

| File | Why |
|---|---|
| `README.md` | CI badge (`madmmas/queriva/actions/workflows/ci.yml`) was added in the foundation commit — points at the workflow created in #1b |

**Manual step after merge:** Enable branch protection on `main` — require check **`CI / Build and test`**.

---

## Summary by layer

| Layer | Issue | Status after #1b |
|---|---|---|
| Monorepo scaffold | #0 | 4 packages build via Turborepo |
| Test tooling | #1 | Unit + integration tests pass locally; 80% coverage on Python/UI |
| CI pipeline | #1b | GitHub Actions runs full `make test` on every PR/push to `main` |
| Business logic | #2+ | Not started — all service classes and loaders are stubs |

## Commands that should all exit 0

```bash
make install
make build
make test          # unit + integration (Docker required for QdrantContainerIT)
make test-unit     # no Docker
make test-int      # Docker required
```
