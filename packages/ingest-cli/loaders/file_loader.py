"""File loader for txt, md, and pdf — implemented in issue #9."""

from typing import Any


def load(base_dir: str, pattern: str = "**/*") -> list[dict[str, Any]]:
    """Loads documents from files under base_dir with path traversal protection."""
    raise NotImplementedError("File loader implemented in issue #9")
