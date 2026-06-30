"""Shared pytest fixtures for embed sidecar tests."""

import pytest

from model_loader import get_loader


@pytest.fixture(autouse=True)
def clear_model_cache() -> None:
    """Clears the in-memory model cache between tests for isolation."""
    get_loader().clear_cache()
    yield
    get_loader().clear_cache()
