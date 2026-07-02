"""CLI subprocess tests for queriva_ingest.py."""

import json
import subprocess
import sys
from pathlib import Path

import pytest

CLI_SCRIPT = Path(__file__).resolve().parent.parent / "queriva_ingest.py"
REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
FIXTURE_PATH = REPO_ROOT / "fixtures" / "news_radar_dhaka_floods.json"


def _run_cli(*extra_args: str) -> subprocess.CompletedProcess[str]:
    """Runs the ingest CLI as a subprocess and returns the completed process."""
    return subprocess.run(
        [sys.executable, str(CLI_SCRIPT), *extra_args],
        capture_output=True,
        text=True,
    )


@pytest.mark.unit
def test_cli_exits_nonzero_when_required_args_are_missing() -> None:
    result = _run_cli()

    assert result.returncode != 0
    assert "--collection" in result.stderr or "required" in result.stderr.lower()


@pytest.mark.unit
def test_cli_exits_nonzero_when_format_is_invalid(tmp_path: Path) -> None:
    source = tmp_path / "article.json"
    source.write_text(
        json.dumps([{"id": "doc-1", "title": "Test", "body": "Body"}]),
        encoding="utf-8",
    )

    result = _run_cli(
        "--collection",
        "news_radar",
        "--format",
        "xml",
        "--source",
        str(source),
    )

    assert result.returncode != 0
    assert "invalid choice" in result.stderr.lower() or "xml" in result.stderr


@pytest.mark.unit
def test_cli_exits_nonzero_on_unknown_flag(tmp_path: Path) -> None:
    source = tmp_path / "article.json"
    source.write_text(
        json.dumps([{"id": "doc-1", "title": "Test", "body": "Body"}]),
        encoding="utf-8",
    )

    result = _run_cli(
        "--collection",
        "news_radar",
        "--format",
        "json",
        "--source",
        str(source),
        "--unknown-flag",
    )

    assert result.returncode != 0
    assert "unrecognized arguments" in result.stderr.lower()


@pytest.mark.unit
def test_cli_exits_nonzero_when_api_is_unreachable(tmp_path: Path) -> None:
    source = tmp_path / "article.json"
    source.write_text(
        json.dumps([{"id": "doc-1", "title": "Test", "body": "Body"}]),
        encoding="utf-8",
    )

    result = _run_cli(
        "--api",
        "http://127.0.0.1:9",
        "--collection",
        "news_radar",
        "--format",
        "json",
        "--source",
        str(source),
    )

    assert result.returncode != 0
    assert "ERROR:" in result.stderr


@pytest.mark.unit
def test_cli_exits_nonzero_when_file_loader_detects_path_traversal(tmp_path: Path) -> None:
    docs_dir = tmp_path / "documents"
    docs_dir.mkdir()

    result = _run_cli(
        "--collection",
        "news_radar",
        "--format",
        "files",
        "--source",
        str(docs_dir / ".." / ".." / ".." / "etc"),
    )

    assert result.returncode != 0
    assert "ERROR:" in result.stderr


@pytest.mark.integration
def test_cli_ingests_fixture_when_api_is_running() -> None:
    if not FIXTURE_PATH.is_file():
        pytest.skip("Fixture file not found at repository root.")

    result = _run_cli(
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
    )

    if result.returncode != 0 and "Failed to reach Queriva API" in result.stderr:
        pytest.skip("Queriva API is not running on localhost:8080")

    assert result.returncode == 0
    assert "Ingest complete" in result.stderr or "Ingest complete" in result.stdout
