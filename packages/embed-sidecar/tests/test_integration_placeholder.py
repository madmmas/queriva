"""Integration placeholder — full HTTP ingest tests live in packages/api (issue #8)."""

import pytest


@pytest.mark.integration
def test_integration_placeholder_runs_without_torch_models() -> None:
    """Placeholder so make test-int does not fail when no sidecar integration tests exist yet."""
    assert True
