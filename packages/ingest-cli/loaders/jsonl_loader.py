"""JSONL loader for newline-delimited document streams (SPEC §7.3)."""

import json
import sys
from contextlib import contextmanager
from pathlib import Path
from typing import Any, Generator, TextIO

from constants import STDIN_SOURCE
from document_utils import validate_documents


def load(source: str, input_stream: TextIO | None = None) -> list[dict[str, Any]]:
    """Loads documents from a JSONL file path or stdin when source is '-'."""
    with _open_stream(source, input_stream) as stream:
        documents: list[dict[str, Any]] = []

        for line_number, line in enumerate(stream, start=1):
            stripped_line = line.strip()
            if not stripped_line:
                continue

            try:
                item = json.loads(stripped_line)
            except json.JSONDecodeError as error:
                raise ValueError(f"Invalid JSON on line {line_number}: {error}") from error

            if not isinstance(item, dict):
                raise ValueError(f"JSONL line {line_number} must be a JSON object.")

            documents.append(item)

        return validate_documents(documents)


@contextmanager
def _open_stream(source: str, input_stream: TextIO | None) -> Generator[TextIO, None, None]:
    """Opens the JSONL input stream from stdin or a file path."""
    if source == STDIN_SOURCE:
        yield input_stream if input_stream is not None else sys.stdin
        return

    path = Path(source)
    if not path.is_file():
        raise ValueError(f"JSONL source file not found: {source}")

    with path.open(encoding="utf-8") as stream:
        yield stream
