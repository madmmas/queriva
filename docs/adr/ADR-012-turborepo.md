# ADR-012 — Monorepo Tooling: Turborepo over Nx

## Status
Accepted

## Context
Queriva is a polyglot monorepo: Java (Spring Boot), Python (FastAPI + CLI),
and TypeScript (React). A monorepo tool is needed to orchestrate builds,
tests, and task pipelines across these four packages with caching and
dependency awareness.

## Decision
Use **Turborepo** as the monorepo build orchestration tool.

## Alternatives Considered

| Option | Reason rejected |
|---|---|
| **Nx** | More powerful monorepo-aware testing (affected commands, project graph). But Nx's generator and plugin ecosystem is heavily JavaScript/TypeScript-centric — Java and Python packages are second-class citizens. Configuration overhead is higher. |
| **Bazel** | Hermetic, reproducible builds at Google scale. Massive learning curve, Java/Python toolchain setup is complex. Overkill for a 4-package project. |
| **Make only** | Simple, universal. But no dependency graph awareness, no caching, no parallelism. Would require hand-wiring `make build-api before make build-ui` etc. |
| **Lerna** | JavaScript monorepo only. Doesn't help with Java or Python packages. |

## Turborepo Rationale

### Zero-config remote caching
Turborepo's remote cache (Vercel-hosted or self-hosted) caches task outputs
by input hash. Running `turbo run test` after a change to `packages/ui` only
retests UI — API and sidecar tests are served from cache. This is significant
for a CI pipeline where `make test` can take 5+ minutes with Testcontainers.

### Simple pipeline definition
```json
// turbo.json
{
  "pipeline": {
    "build": { "dependsOn": ["^build"], "outputs": ["dist/**", "target/**"] },
    "test": { "dependsOn": ["build"] },
    "test-unit": { "dependsOn": ["build"] },
    "test-int":  { "dependsOn": ["build"] }
  }
}
```

Nx requires a more complex `project.json` per package plus a root workspace
config. Turborepo's single `turbo.json` is sufficient for Queriva's pipeline.

### Polyglot support
Turborepo treats each package as a black box — it runs whatever command is
in `package.json` scripts. Java and Python packages are included via wrapper
scripts in `package.json`:
```json
{
  "scripts": {
    "build": "mvn package -q",
    "test": "mvn test"
  }
}
```

This is a lightweight but effective approach. Nx's Java support requires
a plugin with heavier scaffolding.

### AIPlane consistency
AIPlane also uses Turborepo. Using the same tool across both repos reduces
context switching and allows shared tooling patterns.

## Consequences

**Makes easier:**
- `turbo run build` and `turbo run test` work from repo root across all packages
- Task-level caching avoids redundant CI steps
- Pipeline dependencies (`build` before `test`) are explicit and enforced

**Makes harder:**
- Java and Python packages need `package.json` shim files — adds a small
  layer of indirection for developers who only work in Java or Python
- Turborepo remote cache requires a Vercel account or self-hosted cache server
  (local cache still works without a remote — just no cross-machine sharing)

## References
- SPEC.md §5 (repo structure), §16 (tech stack — Turborepo)
- Issue #0 (monorepo scaffold), #1 (test infrastructure — `turbo run test`)
- ADR-004 (Spring Boot), ADR-005 (FastAPI sidecar) — the packages Turborepo orchestrates
