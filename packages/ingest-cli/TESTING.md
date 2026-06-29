# Ingest CLI test conventions

See `test-quality.mdc` Section C.

## Tags

| Marker | When to use | Makefile target |
|---|---|---|
| `@pytest.mark.unit` | Loaders, chunker, argparse | `make test-unit` |
| `@pytest.mark.integration` | Requires running API | `make test-int` |

## Run commands

```bash
make test-ingest
cd packages/ingest-cli && python3 -m pytest -m unit
```

## CLI tests

Test the CLI as a black box via `subprocess.run` — catches argument parsing
errors that function-level tests miss.

## Coverage

`pytest.ini` enforces 80% line coverage via `--cov-fail-under=80`.
