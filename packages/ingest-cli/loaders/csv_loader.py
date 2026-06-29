"""CSV file loader — implemented in issue #9."""

from typing import Any


def load(path: str, column_map: dict[str, str] | None = None) -> list[dict[str, Any]]:
    """Loads documents from a CSV file with optional column mapping."""
    raise NotImplementedError("CSV loader implemented in issue #9")
