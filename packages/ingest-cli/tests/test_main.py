"""Direct unit tests for queriva_ingest main() — complements subprocess tests."""

import argparse
import sys
from pathlib import Path
from typing import Any

import pytest

import queriva_ingest


@pytest.mark.unit
def test_main_returns_zero_when_ingest_succeeds(
        monkeypatch: pytest.MonkeyPatch,
        httpx_mock,
        caplog: pytest.LogCaptureFixture,
) -> None:
    httpx_mock.add_response(
        url="http://localhost:8080/api/ingest/documents",
        json={
            "collection": "news_radar",
            "ingested": 8,
            "chunks_created": 13,
            "skipped": 0,
            "errors": 0,
            "latency_ms": 5,
        },
    )

    monkeypatch.setattr(
        sys,
        "argv",
        [
            "queriva_ingest",
            "--collection",
            "news_radar",
            "--format",
            "json",
            "--source",
            str(_sample_json_path()),
        ],
    )

    with caplog.at_level("INFO"):
        assert queriva_ingest.main() == 0

    assert "Ingest complete" in caplog.text


@pytest.mark.unit
def test_main_returns_one_when_loader_raises_value_error(
        monkeypatch: pytest.MonkeyPatch,
        capsys: pytest.CaptureFixture[str],
) -> None:
    monkeypatch.setattr(
        sys,
        "argv",
        [
            "queriva_ingest",
            "--collection",
            "news_radar",
            "--format",
            "json",
            "--source",
            "/nonexistent/articles.json",
        ],
    )

    assert queriva_ingest.main() == 1
    assert "ERROR:" in capsys.readouterr().err


@pytest.mark.unit
def test_main_returns_one_when_api_returns_conflict(
        monkeypatch: pytest.MonkeyPatch,
        httpx_mock,
        capsys: pytest.CaptureFixture[str],
) -> None:
    httpx_mock.add_response(
        url="http://localhost:8080/api/ingest/documents",
        status_code=409,
        json={"error": "Document already exists"},
    )

    monkeypatch.setattr(
        sys,
        "argv",
        [
            "queriva_ingest",
            "--collection",
            "news_radar",
            "--format",
            "json",
            "--source",
            str(_sample_json_path()),
        ],
    )

    assert queriva_ingest.main() == 1
    assert "Document already exists" in capsys.readouterr().err


@pytest.mark.unit
def test_build_parser_requires_collection_and_format() -> None:
    parser = queriva_ingest.build_parser()

    assert parser.get_default("api_url") == queriva_ingest.DEFAULT_API_URL


@pytest.mark.unit
@pytest.mark.parametrize(
    "format_name,source_key",
    [
        ("json", "json"),
        ("csv", "csv"),
        ("jsonl", "jsonl"),
        ("files", "files"),
    ],
)
def test_load_documents_dispatches_to_each_loader(
        format_name: str,
        source_key: str,
        loader_fixtures: dict[str, Any],
) -> None:
    args = argparse.Namespace(
        format=format_name,
        source=loader_fixtures[source_key]["path"],
        map=[],
    )

    documents = queriva_ingest.load_documents(args)

    assert len(documents) == loader_fixtures[source_key]["expected_count"]


@pytest.mark.unit
def test_load_documents_dispatches_to_url_loader(
        loader_fixtures: dict[str, Any],
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(
        queriva_ingest.url_loader,
        "load_from_file",
        lambda _path: [{"id": "url-1", "title": "Fetched", "body": "Text"}],
    )

    args = argparse.Namespace(
        format="urls",
        source=loader_fixtures["urls"]["path"],
        map=[],
    )

    documents = queriva_ingest.load_documents(args)

    assert documents[0]["title"] == "Fetched"


@pytest.mark.unit
def test_load_documents_raises_for_unsupported_format() -> None:
    args = argparse.Namespace(format="unsupported", source=".", map=[])

    with pytest.raises(ValueError, match="Unsupported format"):
        queriva_ingest.load_documents(args)


def _sample_json_path() -> str:
    fixture = (
        Path(__file__).resolve().parent.parent.parent.parent
        / "fixtures"
        / "news_radar_dhaka_floods.json"
    )
    return str(fixture)
