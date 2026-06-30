"""Unit tests for embed sidecar health endpoint."""

from unittest.mock import MagicMock, patch

import numpy as np
import pytest
from httpx import ASGITransport, AsyncClient

from main import app

HEALTH_PATH = "/api/health"
EMBED_PATH = "/api/embed"


@pytest.mark.unit
@pytest.mark.asyncio
async def test_health_returns_ok_status_and_empty_models_list_when_no_models_loaded() -> None:
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get(HEALTH_PATH)

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["models_loaded"] == []


@pytest.mark.unit
@pytest.mark.asyncio
async def test_health_response_matches_spec_contract() -> None:
    """SPEC §9 — GET /api/health returns status and models_loaded."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get(HEALTH_PATH)

    assert response.status_code == 200
    body = response.json()
    assert set(body.keys()) == {"status", "models_loaded"}
    assert isinstance(body["status"], str)
    assert isinstance(body["models_loaded"], list)


@pytest.mark.unit
@pytest.mark.asyncio
@patch("model_loader.SentenceTransformer")
async def test_health_lists_loaded_models_after_embed(
    mock_sentence_transformer: MagicMock,
) -> None:
    mock_model = MagicMock()
    mock_model.encode.return_value = np.zeros(768, dtype=np.float32)
    mock_sentence_transformer.return_value = mock_model

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        await client.post(EMBED_PATH, json={"text": "hello", "model": "LaBSE"})
        response = await client.get(HEALTH_PATH)

    assert response.status_code == 200
    assert response.json()["models_loaded"] == ["LaBSE"]
