"""Unit tests for document_utils."""

import pytest

from document_utils import validate_documents


@pytest.mark.unit
def test_validate_documents_rejects_empty_list() -> None:
    with pytest.raises(ValueError, match="No documents loaded"):
        validate_documents([])


@pytest.mark.unit
def test_validate_documents_rejects_missing_title() -> None:
    with pytest.raises(ValueError, match="missing required field 'title'"):
        validate_documents([{"id": "doc-1"}])
