"""Document validation helpers for ingest CLI loaders."""

from typing import Any

from constants import REQUIRED_DOCUMENT_FIELDS


def validate_documents(documents: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Validates that every document has required fields before ingest."""
    if not documents:
        raise ValueError("No documents loaded from input source.")

    for index, document in enumerate(documents):
        for field_name in REQUIRED_DOCUMENT_FIELDS:
            value = document.get(field_name)
            if value is None or (isinstance(value, str) and not value.strip()):
                raise ValueError(
                    f"Document at index {index} is missing required field '{field_name}'."
                )

    return documents
