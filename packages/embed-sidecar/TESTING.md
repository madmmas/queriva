# Embed sidecar test conventions

See `test-quality.mdc` Section C.

## Tags

| Marker | When to use | Makefile target |
|---|---|---|
| `@pytest.mark.unit` | No Docker, mocked HTTP | `make test-unit` |
| `@pytest.mark.integration` | Real model loading or sidecar process | `make test-int` |
| `@pytest.mark.slow` | Large model download tests | manual |

## Run commands

```bash
make test-embed
cd packages/embed-sidecar && python3 -m pytest -m unit
cd packages/embed-sidecar && python3 -m pytest -m integration
```

## HTTP mocking

Use `respx` for mocking outbound HTTP. Use `httpx.AsyncClient` with
`ASGITransport` for in-process FastAPI tests.

## Coverage

`pytest.ini` enforces 90% line coverage via `--cov-fail-under=90`.
