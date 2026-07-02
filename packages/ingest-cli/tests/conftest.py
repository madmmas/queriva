"""Shared pytest fixtures for ingest-cli loader tests."""

import json
from io import StringIO
from pathlib import Path
from typing import Any, Callable

import pytest


@pytest.fixture
def sample_document() -> dict[str, str]:
    """Returns a minimal valid document for loader fixtures."""
    return {
        "id": "cluster-001",
        "title": "Buriganga river overflows, floods low-lying Dhaka areas",
        "body": "Heavy monsoon rains caused the Buriganga river to overflow its banks.",
        "source": "prothomalo.com",
        "language": "bn",
    }


@pytest.fixture
def loader_fixtures(tmp_path: Path, sample_document: dict[str, str]) -> dict[str, Any]:
    """Creates on-disk fixtures for every supported loader format."""
    json_path = tmp_path / "articles.json"
    json_path.write_text(json.dumps([sample_document]), encoding="utf-8")

    csv_path = tmp_path / "articles.csv"
    csv_path.write_text(
        "id,title,body,source,language\n"
        f"{sample_document['id']},{sample_document['title']},"
        f"{sample_document['body']},{sample_document['source']},{sample_document['language']}\n",
        encoding="utf-8",
    )

    jsonl_path = tmp_path / "articles.jsonl"
    jsonl_path.write_text(
        json.dumps(sample_document) + "\n"
        + json.dumps({**sample_document, "id": "cluster-002", "title": "Second article"}) + "\n",
        encoding="utf-8",
    )

    files_dir = tmp_path / "documents"
    files_dir.mkdir()
    (files_dir / "flood-report.txt").write_text(sample_document["body"], encoding="utf-8")

    urls_path = tmp_path / "urls.txt"
    urls_path.write_text("https://example.com/flood-article\n", encoding="utf-8")

    return {
        "json": {"path": str(json_path), "expected_count": 1},
        "csv": {"path": str(csv_path), "expected_count": 1},
        "jsonl": {"path": str(jsonl_path), "expected_count": 2},
        "files": {"path": str(files_dir), "expected_count": 1},
        "urls": {"path": str(urls_path), "expected_count": 1},
        "sample_document": sample_document,
    }


@pytest.fixture
def resolve_public_ip(monkeypatch: pytest.MonkeyPatch) -> Callable[[str], str]:
    """Patches DNS resolution to a public IP for URL loader tests."""
    def resolver(_hostname: str) -> str:
        return "93.184.216.34"

    monkeypatch.setattr("loaders.url_loader.socket.gethostbyname", resolver)
    return resolver
