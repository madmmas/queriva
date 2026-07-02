"""Unit tests for scripts/validate_fixture.py."""

import json
import subprocess
import sys
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
SCRIPTS_DIR = REPO_ROOT / "scripts"
FIXTURE_PATH = REPO_ROOT / "fixtures" / "news_radar_dhaka_floods.json"
VALIDATE_SCRIPT = SCRIPTS_DIR / "validate_fixture.py"


@pytest.mark.unit
def test_validate_fixture_script_accepts_news_radar_fixture() -> None:
    result = subprocess.run(
        [sys.executable, str(VALIDATE_SCRIPT), str(FIXTURE_PATH)],
        capture_output=True,
        text=True,
        cwd=REPO_ROOT,
    )

    assert result.returncode == 0
    assert "Fixture OK" in result.stdout


@pytest.mark.unit
def test_validate_fixture_script_rejects_missing_required_field(tmp_path: Path) -> None:
    broken_fixture = tmp_path / "broken.json"
    broken_fixture.write_text(
        json.dumps([{"id": "doc-1", "title": "Missing body"}]),
        encoding="utf-8",
    )

    result = subprocess.run(
        [sys.executable, str(VALIDATE_SCRIPT), str(broken_fixture)],
        capture_output=True,
        text=True,
    )

    assert result.returncode != 0
    assert "missing required field 'body'" in result.stderr
