# Queriva Ingest CLI

Batch ingestion tool for files, JSON, CSV, URLs, and JSONL streams.
Posts documents to `POST /api/ingest/documents` on the Queriva API (SPEC §7.3).

## Setup

```bash
cd packages/ingest-cli
python3 -m pip install -r requirements.txt
```

## Usage

### JSON

```bash
python queriva_ingest.py --api http://localhost:8080 \
  --collection news_radar --format json --source ../../fixtures/news_radar_dhaka_floods.json
```

### CSV with column mapping

```bash
python queriva_ingest.py --api http://localhost:8080 \
  --collection news_radar --format csv --source ./articles.csv \
  --map title=headline body=content source=outlet published_at=date
```

### Directory of files

Recursively ingests `.txt`, `.md`, and `.pdf` files:

```bash
python queriva_ingest.py --api http://localhost:8080 \
  --collection my_docs --format files --source ./documents/
```

### URL list

One URL per line in the source file. Private and loopback addresses are blocked (SSRF prevention).

```bash
python queriva_ingest.py --api http://localhost:8080 \
  --collection my_web --format urls --source ./urls.txt
```

### Stdin JSONL

```bash
cat documents.jsonl | python queriva_ingest.py --api http://localhost:8080 \
  --collection news_radar --format jsonl --source -
```

## Options

| Flag | Default | Description |
|---|---|---|
| `--api` | `http://localhost:8080` | Queriva API base URL |
| `--collection` | _(required)_ | Target collection name |
| `--format` | _(required)_ | `json`, `csv`, `files`, `urls`, or `jsonl` |
| `--source` | _(required)_ | Input path, or `-` for jsonl stdin |
| `--map` | — | CSV column mapping (`field=column`) |
| `--model` | `LaBSE` | Embedding model sent to the API |
| `--upsert-mode` | `skip_existing` | `skip_existing`, `overwrite`, or `error_on_conflict` |
| `--no-chunking` | off | Disable server-side chunking for this request |

## Security

- **Path traversal:** `file_loader` rejects paths that escape the source directory.
- **SSRF:** `url_loader` blocks URLs resolving to private, loopback, or link-local addresses.
- **No ML imports:** all embedding is done by the API / embed-sidecar.

## Tests

```bash
make test-ingest
```
