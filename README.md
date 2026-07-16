<p align="center">
  <img src="mock-ui/queriva-icon-files/queriva-icon-light.svg" alt="Queriva" width="72" height="72">
</p>

<h1 align="center">Queriva</h1>

<p align="center">
  <a href="https://github.com/madmmas/queriva/actions/workflows/ci.yml"><img src="https://github.com/madmmas/queriva/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-teal.svg" alt="License: MIT"></a>
</p>

<p align="center"><strong>Ask your data anything.</strong></p>

<p align="center">
Self-hosted, LLM-powered semantic search. Ingest documents from any source,
search in natural language, get AI-synthesized answers — zero external API calls.
</p>

<p align="center">
  <img src="mock-ui/queriva-icon-files/ui.png" alt="Queriva standalone UI — floods in Dhaka search with RAG mode" width="900">
</p>

> **Status:** v0.4.0+ — full Docker Compose stack (qdrant, ollama, embed-sidecar, api, ui).
> See [`docs/SPEC.md`](docs/SPEC.md) for architecture and [`docs/ISSUES.md`](docs/ISSUES.md)
> for the backlog (next: #28 E2E smoke → v0.5.0).

## Quick start

```bash
docker compose up -d --build   # or: make compose-up
make ollama-pull               # first run only — downloads mistral (~4GB) for RAG mode
make seed                      # ingest fixtures/news_radar_dhaka_floods.json into news_radar
```

Then open **http://localhost:3000** and search `floods in Dhaka last week`.

`make seed` validates the fixture, ingests 8 articles (4 Bangla + 4 English), and verifies
idempotency. Requires API, embed-sidecar, and Qdrant from `docker compose`.

### Dev compose (hot reload)

```bash
make compose-dev    # UI Vite HMR + API mvn spring-boot:run
```

## Docs

- [`docs/SPEC.md`](docs/SPEC.md) — full specification
- [`docs/ISSUES.md`](docs/ISSUES.md) — implementation backlog
- [`docs/adr/`](docs/adr/) — architecture decision records
- [`CHANGELOG.md`](CHANGELOG.md) — version history

Full README coming at v1.0.0 — see [`README-TEMPLATE.md`](README-TEMPLATE.md).
