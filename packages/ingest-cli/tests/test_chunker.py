"""Verifies chunker stub raises NotImplementedError until issue #6."""

import pytest

from chunker import chunk


@pytest.mark.unit
def test_chunker_raises_not_implemented_when_called() -> None:
    with pytest.raises(NotImplementedError, match="issue #6"):
        chunk("Title", "Body text")
