# Queriva — Development Journal

Personal log of daily progress, blockers, and decisions.
One entry per working day. Most recent entry at the top.

---

## 2026-06-29 — Issue #1: Test infrastructure

Branch: `issue-1/test-infrastructure`
Depends on: #0, ADR-009
Blocks: #1b, #3, #8, #10, #15, #18, #24, #26, #28

### Built

**packages/api**
- JUnit 5 + Spring Boot Test + Testcontainers (Qdrant) + AssertJ + WireMock 3.10
- Maven profiles: `-Punit` (no Docker), `-Pintegration` (Testcontainers)
- Placeholder tests: `QuerivaApplicationTest`, `WireMockSetupTest`, `QdrantContainerIT`
- `TESTING.md` documenting tag conventions

**packages/embed-sidecar**
- pytest + pytest-asyncio + pytest-cov + httpx AsyncClient + respx
- Placeholder tests: health endpoint (async), respx mock pattern, integration stub
- `pytest.ini` with 80% coverage floor

**packages/ingest-cli**
- pytest + pytest-cov + subprocess CLI tests + direct `main()` unit tests
- Loader and chunker stub tests
- `pytest.ini` with 80% coverage floor

**packages/ui**
- Vitest + React Testing Library + MSW + jest-axe
- `App.test.tsx` — render + accessibility audit
- Separate `vitest.config.ts`; stub components excluded from coverage until #19–#25

**Root**
- Makefile `install-python` installs `requirements-dev.txt` for both Python packages
- All Makefile test targets run real tests (removed `|| true` fallbacks)

**Verification**
- `make test-unit` → exit 0, no Docker
- `make test-int` → exit 0, Testcontainers pulls `qdrant/qdrant:latest`
- `make test` → exit 0 (all packages)

### Blocked
- UI jest-axe failed on duplicate `<main>` landmark — fixed with `afterEach(cleanup)`
- WireMock artifact `wiremock-junit5` not on Maven Central — switched to `wiremock-standalone`
- Python `test-int` failed coverage gate when only integration markers ran — use `--no-cov` for test-int

### Decided
- Integration test runs skip coverage (`--no-cov`) — coverage enforced on `test` and `test-unit` only
- UI stub components excluded from Vitest coverage until implemented in later issues
- WireMock 3.x verify API uses `getRequestedFor()` not `get()`

### Tomorrow
- Start issue #1b: GitHub Actions CI pipeline
- Branch: `issue-1b/github-actions-ci`

---

## 2026-06-29 (issue #0)

### Built
- Issue #0: scaffolded Turborepo monorepo per SPEC §5
- Created packages/api (Spring Boot 3.4 / Java 21), embed-sidecar (FastAPI), ingest-cli, ui (React 18 + Vite)
- Root package.json, turbo.json, Makefile, .env.example
- `turbo run build` exits 0 across all four packages

### Blocked
- Nothing

### Decided
- Python packages use `python3` in npm scripts (macOS has no `python` shim)

### Tomorrow
- Start issue #1: test infrastructure (JUnit, pytest, Vitest + MSW)

---

## 2026-06-29

### Built
- Created GitHub repo: madmmas/queriva
- Copied all 25 foundation files (SPEC, ISSUES, ADRs, Cursor rules, fixture, seed script)
- Set up .gitignore, README stub, seed script permissions

### Blocked
- Nothing yet

### Decided
- Starting fresh repo rather than retrofitting old one
- All 12 ADRs already written — #meta issue is effectively pre-done

### Tomorrow
- Start issue #0: scaffold Turborepo monorepo (packages/api, embed-sidecar, ingest-cli, ui)

---

## 2026-06-29

### Built
- All 12 ADRs written and committed in foundation commit
- Closed #meta issue
- CHANGELOG.md updated with all ADR entries

### Blocked
- Nothing

### Decided
- Single commit for all foundation files is cleaner than 12 separate ADR commits
  at this stage — the repo is brand new and no CI is running yet

### Tomorrow
- Start issue #0: scaffold Turborepo monorepo
- Create branch: issue-0/scaffold-monorepo
- Open Cursor, say: "Start issue #0"


---

Format:
```
## YYYY-MM-DD
### Built
### Blocked
### Decided
### Tomorrow
```

---

## YYYY-MM-DD

### Built
- (What was completed today — issue number, specific component)

### Blocked
- (What slowed you down or stopped you — be specific)

### Decided
- (Any micro-decision made today not big enough for an ADR)

### Tomorrow
- (The one thing to start with first thing tomorrow)

---

<!--
TIPS:
- Write this at end of day, takes 5 minutes
- "Blocked" appearing 3+ days in a row = stop and resolve it
- "Decided" entries are seeds for blog posts
- "Tomorrow" is your morning standup with yourself
-->
