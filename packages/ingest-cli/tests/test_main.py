"""Direct unit tests for queriva_ingest main() — complements subprocess tests."""

import sys

import pytest

import queriva_ingest


@pytest.mark.unit
def test_main_returns_zero_when_valid_arguments_are_provided(monkeypatch: pytest.MonkeyPatch) -> None:
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
            "fixtures/data.json",
        ],
    )

    assert queriva_ingest.main() == 0


@pytest.mark.unit
def test_build_parser_requires_collection_and_format() -> None:
    parser = queriva_ingest.build_parser()

    assert parser.get_default("api_url") == queriva_ingest.DEFAULT_API_URL
