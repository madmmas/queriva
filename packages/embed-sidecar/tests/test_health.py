"""Unit tests for embed sidecar health endpoint."""

import pytest
from httpx import ASGITransport, AsyncClient

from main import app

HEALTH_PATH = "/api/health"


@pytest.mark.unit
@pytest.mark.asyncio
async def test_health_returns_ok_status_and_empty_models_list() -> None:
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get(HEALTH_PATH)

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["models_loaded"] == []
