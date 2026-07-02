"""CSV file loader with column mapping (SPEC §7.3)."""

import csv
from pathlib import Path
from typing import Any

from document_utils import validate_documents

DEFAULT_COLUMN_MAP = {
    "id": "id",
    "title": "title",
    "body": "body",
    "source": "source",
    "language": "language",
    "published_at": "published_at",
    "category": "category",
    "url": "url",
    "cluster_id": "cluster_id",
}


def load(path: str, column_map: dict[str, str] | None = None) -> list[dict[str, Any]]:
    """Loads documents from a CSV file using the provided column mapping."""
    source_path = Path(path)
    if not source_path.is_file():
        raise ValueError(f"CSV source file not found: {path}")

    effective_map = DEFAULT_COLUMN_MAP if column_map is None else column_map
    documents: list[dict[str, Any]] = []

    with source_path.open(encoding="utf-8", newline="") as csv_file:
        reader = csv.DictReader(csv_file)
        if reader.fieldnames is None:
            raise ValueError(f"CSV source '{path}' has no header row.")

        for row_index, row in enumerate(reader, start=2):
            document = _map_row(row, effective_map, row_index)
            if document:
                documents.append(document)

    return validate_documents(documents)


def _map_row(
        row: dict[str, str | None],
        column_map: dict[str, str],
        row_index: int,
) -> dict[str, Any] | None:
    """Maps one CSV row to a document object, skipping empty rows."""
    document: dict[str, Any] = {}

    for document_field, csv_column in column_map.items():
        if csv_column not in row:
            continue

        cell_value = row.get(csv_column)
        if cell_value is not None and cell_value.strip():
            document[document_field] = cell_value.strip()

    if not document:
        return None

    return document
