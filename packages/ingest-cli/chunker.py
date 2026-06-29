"""Character-based sliding window chunker — implemented in issue #6 (API) / #9 (CLI)."""

DEFAULT_CHUNK_SIZE = 512
DEFAULT_OVERLAP = 64


def chunk(title: str, body: str, chunk_size: int = DEFAULT_CHUNK_SIZE, overlap: int = DEFAULT_OVERLAP) -> list[str]:
    """Splits document body into overlapping chunks with title prepended."""
    raise NotImplementedError("Chunker implemented in issue #6")
