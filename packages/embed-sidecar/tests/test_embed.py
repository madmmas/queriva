"""Unit tests for POST /api/embed."""

from unittest.mock import MagicMock, patch

import numpy as np
import pytest
from httpx import ASGITransport, AsyncClient

from main import app

EMBED_PATH = "/api/embed"

SUPPORTED_MODEL_CASES = [
    ("LaBSE", 768),
    ("all-MiniLM-L6-v2", 384),
    ("paraphrase-multilingual-mpnet-base-v2", 768),
]


@pytest.mark.unit
@pytest.mark.parametrize("model_name,expected_dimensions", SUPPORTED_MODEL_CASES)
@pytest.mark.asyncio
@patch("model_loader.SentenceTransformer")
async def test_embed_returns_correct_dimensions_for_supported_model(
    mock_sentence_transformer: MagicMock,
    model_name: str,
    expected_dimensions: int,
) -> None:
    mock_model = MagicMock()
    mock_model.encode.return_value = np.zeros(expected_dimensions, dtype=np.float32)
    mock_sentence_transformer.return_value = mock_model

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            EMBED_PATH,
            json={"text": "floods in Dhaka last week", "model": model_name},
        )

    body = response.json()
    assert response.status_code == 200
    assert body["dimensions"] == expected_dimensions
    assert len(body["vector"]) == expected_dimensions
    assert all(isinstance(value, float) for value in body["vector"])
    mock_sentence_transformer.assert_called_once()


@pytest.mark.unit
@pytest.mark.asyncio
async def test_embed_returns_422_when_text_is_empty() -> None:
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(EMBED_PATH, json={"text": "", "model": "LaBSE"})

    assert response.status_code == 422


@pytest.mark.unit
@pytest.mark.asyncio
async def test_embed_returns_503_when_model_name_is_invalid() -> None:
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            EMBED_PATH,
            json={"text": "hello", "model": "unknown-model"},
        )

    assert response.status_code == 503
    assert "not supported" in response.json()["detail"]
    assert "LaBSE" in response.json()["detail"]


@pytest.mark.unit
@pytest.mark.asyncio
@patch("model_loader.SentenceTransformer")
async def test_model_loads_only_once_per_name_when_embed_called_twice(
    mock_sentence_transformer: MagicMock,
) -> None:
    mock_model = MagicMock()
    mock_model.encode.return_value = np.zeros(768, dtype=np.float32)
    mock_sentence_transformer.return_value = mock_model

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        first = await client.post(EMBED_PATH, json={"text": "hello", "model": "LaBSE"})
        second = await client.post(EMBED_PATH, json={"text": "world", "model": "LaBSE"})

    assert first.status_code == 200
    assert second.status_code == 200
    mock_sentence_transformer.assert_called_once()


@pytest.mark.unit
@pytest.mark.asyncio
@patch("model_loader.SentenceTransformer", side_effect=OSError("download failed"))
async def test_embed_returns_503_when_model_download_fails(
    _mock_sentence_transformer: MagicMock,
) -> None:
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            EMBED_PATH,
            json={"text": "hello", "model": "LaBSE"},
        )

    assert response.status_code == 503
    assert "could not be loaded" in response.json()["detail"]


@pytest.mark.unit
@pytest.mark.asyncio
@patch("model_loader.SentenceTransformer")
async def test_embed_response_matches_spec_contract(
    mock_sentence_transformer: MagicMock,
) -> None:
    """SPEC §6 — POST /api/embed returns vector and dimensions."""
    mock_model = MagicMock()
    mock_model.encode.return_value = np.zeros(768, dtype=np.float32)
    mock_sentence_transformer.return_value = mock_model

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            EMBED_PATH,
            json={"text": "floods in Dhaka last week", "model": "LaBSE"},
        )

    assert response.status_code == 200
    body = response.json()
    assert set(body.keys()) == {"vector", "dimensions"}
    assert isinstance(body["vector"], list)
    assert isinstance(body["dimensions"], int)
    assert body["dimensions"] == 768
