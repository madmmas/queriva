"""CLI subprocess tests for queriva_ingest.py."""

import json
import subprocess
import sys
from pathlib import Path

import pytest

CLI_SCRIPT = Path(__file__).resolve().parent.parent / "queriva_ingest.py"
REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
FIXTURE_PATH = REPO_ROOT / "fixtures" / "news_radar_dhaka_floods.json"


@pytest.mark.unit
def test_cli_exits_nonzero_when_required_args_are_missing() -> None:
    result = subprocess.run(
        [sys.executable, str(CLI_SCRIPT)],
        capture_output=True,
        text=True,
    )

    assert result.returncode != 0
    assert "--collection" in result.stderr or "required" in result.stderr.lower()


@pytest.mark.unit
def test_cli_exits_nonzero_when_api_is_unreachable(tmp_path: Path) -> None:
    source = tmp_path / "article.json"
    source.write_text(
        json.dumps([{"id": "doc-1", "title": "Test", "body": "Body"}]),
        encoding="utf-8",
    )

    result = subprocess.run(
        [
            sys.executable,
            str(CLI_SCRIPT),
            "--api",
            "http://127.0.0.1:9",
            "--collection",
            "news_radar",
            "--format",
            "json",
            "--source",
            str(source),
        ],
        capture_output=True,
        text=True,
    )

    assert result.returncode != 0
    assert "ERROR:" in result.stderr


@pytest.mark.integration
def test_cli_ingests_fixture_when_api_is_running() -> None:
    if not FIXTURE_PATH.is_file():
        pytest.skip("Fixture file not found at repository root.")

    result = subprocess.run(
        [
            sys.executable,
            str(CLI_SCRIPT),
            "--api",
            "http://localhost:8080",
            "--collection",
            "news_radar_cli_it",
            "--format",
            "json",
            "--source",
            str(FIXTURE_PATH),
            "--upsert-mode",
            "skip_existing",
        ],
        capture_output=True,
        text=True,
    )

    if result.returncode != 0 and "Failed to reach Queriva API" in result.stderr:
        pytest.skip("Queriva API is not running on localhost:8080")

    assert result.returncode == 0
    assert "Ingest complete" in result.stderr or "Ingest complete" in result.stdout
