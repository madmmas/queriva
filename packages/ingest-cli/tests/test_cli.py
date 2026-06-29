"""CLI subprocess tests for queriva_ingest.py."""

import subprocess
import sys
from pathlib import Path

import pytest

CLI_SCRIPT = Path(__file__).resolve().parent.parent / "queriva_ingest.py"


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
def test_cli_exits_zero_when_valid_args_are_provided() -> None:
    result = subprocess.run(
        [
            sys.executable,
            str(CLI_SCRIPT),
            "--collection",
            "news_radar",
            "--format",
            "json",
            "--source",
            "fixtures/news_radar_dhaka_floods.json",
        ],
        capture_output=True,
        text=True,
        cwd=Path(__file__).resolve().parent.parent.parent.parent,
    )

    assert result.returncode == 0
