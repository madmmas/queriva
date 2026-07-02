"""Named constants for the ingest CLI (code-quality.mdc C3)."""

import os

DEFAULT_API_URL = os.getenv("QUERIVA_API_URL", "http://localhost:8080")
DEFAULT_MODEL = os.getenv("EMBED_DEFAULT_MODEL", "LaBSE")
DEFAULT_UPSERT_MODE = os.getenv("INGEST_DEFAULT_UPSERT_MODE", "skip_existing")
DEFAULT_CHUNK_SIZE = int(os.getenv("INGEST_DEFAULT_CHUNK_SIZE", "512"))
DEFAULT_OVERLAP = int(os.getenv("INGEST_DEFAULT_OVERLAP", "64"))

HTTP_CONNECT_TIMEOUT_SECONDS = float(os.getenv("HTTP_CONNECT_TIMEOUT_SECONDS", "5"))
HTTP_READ_TIMEOUT_SECONDS = float(os.getenv("HTTP_READ_TIMEOUT_SECONDS", "30"))

SUPPORTED_FILE_SUFFIXES = (".txt", ".md", ".pdf")
STDIN_SOURCE = "-"

REQUIRED_DOCUMENT_FIELDS = ("id", "title")
