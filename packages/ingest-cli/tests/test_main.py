"""Direct unit tests for queriva_ingest main() — complements subprocess tests."""

import sys
from pathlib import Path

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
def test_build_parser_requires_collection_and_format() -> None:
    parser = queriva_ingest.build_parser()

    assert parser.get_default("api_url") == queriva_ingest.DEFAULT_API_URL


def _sample_json_path() -> str:
    fixture = (
        Path(__file__).resolve().parent.parent.parent.parent
        / "fixtures"
        / "news_radar_dhaka_floods.json"
    )
    return str(fixture)
