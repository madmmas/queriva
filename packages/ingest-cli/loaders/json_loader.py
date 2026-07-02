"""JSON file loader for document arrays (SPEC §7.3)."""

import json
from pathlib import Path
from typing import Any

from document_utils import validate_documents


def load(path: str) -> list[dict[str, Any]]:
    """Loads documents from a JSON file containing an array of document objects."""
    source_path = Path(path)
    if not source_path.is_file():
        raise ValueError(f"JSON source file not found: {path}")

    try:
        payload = json.loads(source_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        raise ValueError(f"Invalid JSON in '{path}': {error}") from error

    if not isinstance(payload, list):
        raise ValueError(f"JSON source '{path}' must contain an array of document objects.")

    documents: list[dict[str, Any]] = []
    for index, item in enumerate(payload):
        if not isinstance(item, dict):
            raise ValueError(f"Document at index {index} in '{path}' must be a JSON object.")
        documents.append(item)

    return validate_documents(documents)
