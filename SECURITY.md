# Security

## Scope

Queriva is a local-first, self-hosted tool. In its default configuration it
runs entirely on localhost with no authentication layer and no exposure to
the internet. The security surface is therefore limited, but the following
areas are in scope for responsible disclosure:

- Input injection via Qdrant filter fields
- Path traversal in the `ingest-cli` file and URL loaders
- SSRF (Server-Side Request Forgery) in the URL loader
- Dependency vulnerabilities in any package

## Out of scope

- Issues that require physical access to the machine running Queriva
- Issues in Qdrant, Ollama, or sentence-transformers themselves
  (report these to their respective maintainers)
- Authentication or multi-tenancy — these are non-goals in v1 (see `docs/SPEC.md §3`)

## Reporting a vulnerability

Please do not open a public GitHub Issue for security vulnerabilities.

Report privately via GitHub's private vulnerability reporting:
`https://github.com/madmmas/queriva/security/advisories/new`

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if you have one)

You will receive a response within 7 days.

## Supported versions

Only the latest release tag is supported with security fixes.

## Known limitations (by design)

- No authentication in v1 — Queriva is intended to run behind a firewall
  or on localhost only. Do not expose port 8080 or 3000 to the internet.
- The Qdrant instance is unauthenticated by default. Set `QDRANT_API_KEY`
  if running in a shared environment.
- The Ollama instance is unauthenticated by default.
