"""File loader for txt, md, and pdf with path traversal protection (SPEC §7.3, E3)."""

import hashlib
from pathlib import Path
from typing import Any

from constants import SUPPORTED_FILE_SUFFIXES
from document_utils import validate_documents


def safe_path(base_dir: str, user_path: str) -> Path:
    """Resolves a user path under base_dir and rejects path traversal attempts."""
    base_resolved = Path(base_dir).resolve()
    resolved = (base_resolved / user_path).resolve()

    if not str(resolved).startswith(str(base_resolved)):
        raise ValueError(f"Path traversal detected: {user_path}")

    return resolved


def load(base_dir: str) -> list[dict[str, Any]]:
    """Loads documents recursively from .txt, .md, and .pdf files under base_dir."""
    base_resolved = Path(base_dir).resolve()
    if not base_resolved.is_dir():
        raise ValueError(f"File source directory not found: {base_dir}")

    documents: list[dict[str, Any]] = []

    for suffix in SUPPORTED_FILE_SUFFIXES:
        for file_path in sorted(base_resolved.rglob(f"*{suffix}")):
            if not file_path.is_file():
                continue

            relative_path = file_path.relative_to(base_resolved)
            safe_path(str(base_resolved), str(relative_path))

            body = _read_file_body(file_path)
            if not body.strip():
                continue

            documents.append({
                "id": _document_id_for_path(relative_path),
                "title": file_path.stem,
                "body": body,
                "source": str(relative_path.parent) if str(relative_path.parent) != "." else "files",
                "language": "en",
            })

    return validate_documents(documents)


def _document_id_for_path(relative_path: Path) -> str:
    """Derives a stable document id from a relative file path."""
    normalized = str(relative_path).replace("\\", "/")
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:16]


def _read_file_body(file_path: Path) -> str:
    """Reads text content from a supported file type."""
    suffix = file_path.suffix.lower()

    if suffix in {".txt", ".md"}:
        return file_path.read_text(encoding="utf-8")

    if suffix == ".pdf":
        return _read_pdf_text(file_path)

    raise ValueError(f"Unsupported file type '{suffix}' in file loader.")


def _read_pdf_text(file_path: Path) -> str:
    """Extracts text from a PDF file using pypdf."""
    try:
        from pypdf import PdfReader
    except ImportError as error:
        raise RuntimeError(
            "pypdf is required to ingest PDF files. Install ingest-cli requirements.txt."
        ) from error

    try:
        reader = PdfReader(str(file_path))
        pages = [page.extract_text() or "" for page in reader.pages]
        return "\n".join(pages).strip()
    except OSError as error:
        raise ValueError(f"Failed to read PDF '{file_path}': {error}") from error
