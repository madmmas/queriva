# Contributing

Queriva is currently developed and maintained by a single developer.
This file documents the personal workflow conventions used on this project.

---

## Workflow

All work flows through GitHub Issues. The full ordered backlog is in
`docs/ISSUES.md`. Issues are worked in dependency order — never start
an issue until its dependencies are closed.

```
Pick issue from docs/ISSUES.md
    ↓
Create branch: git checkout -b issue-N/short-description
    ↓
Write ADR (if mapped in docs/SPEC.md §19)
    ↓
Implement tasks from issue checklist
    ↓
Write tests (every item in the issue test plan)
    ↓
make test → exits 0
    ↓
Update CHANGELOG.md under [Unreleased]
    ↓
Write JOURNAL.md entry
    ↓
Commit: git commit -m "[issue-N] description"
    ↓
Push + merge to main
```

---

## Branch naming

```
issue-N/short-description
adr-NNN/short-title        ← ADR-only commits
```

Examples:
```
issue-6/chunking-service
issue-14/search-api
adr-003/chunking-strategy
```

---

## Commit format

```
[issue-N] description
[adr-NNN] ADR title
[spec] updated §N — reason
release: vX.Y.Z
```

---

## What never goes into a commit

- `.env` — only `.env.example` is committed
- Binary files: `.jar`, `.pyc`, model weights
- Build artifacts: `target/`, `dist/`, `__pycache__`, `node_modules/`
- IDE config other than `.cursor/`

---

## CI and branch protection

Every push to `main` and every pull request runs `.github/workflows/ci.yml`:

```
make install → make build → make test
```

Toolchain: Node 20, Java 21, Python 3.11 on `ubuntu-latest` with Docker
(Testcontainers for Qdrant integration tests).

**Branch protection (GitHub repo settings):** require the `CI / Build and test`
check to pass before merging to `main`. Configure manually under
*Settings → Branches → Branch protection rules*.

---

## Releases

Releases follow the version guide in `docs/SPEC.md §18`.
Each release has a matching GitHub release with CHANGELOG notes and a Loom demo.

---

## External contributions

Not accepted at this stage (v0.x). This will change at v1.0.0.
Issues and bug reports via GitHub Issues are welcome.
