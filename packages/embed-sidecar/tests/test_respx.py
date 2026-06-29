"""Unit tests demonstrating respx HTTP mocking for future sidecar clients."""

import httpx
import pytest
import respx


@pytest.mark.unit
@pytest.mark.asyncio
@respx.mock
async def test_respx_mocks_outbound_http_requests() -> None:
    respx.get("http://example.com/api/status").mock(
        return_value=httpx.Response(200, json={"ready": True}),
    )

    async with httpx.AsyncClient() as client:
        response = await client.get("http://example.com/api/status")

    assert response.status_code == 200
    assert response.json()["ready"] is True
