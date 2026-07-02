"""Unit tests for api_client."""

import pytest

from api_client import ingest_documents


@pytest.mark.unit
def test_ingest_documents_posts_payload_to_api(httpx_mock) -> None:
    httpx_mock.add_response(
        url="http://localhost:8080/api/ingest/documents",
        json={
            "collection": "news_radar",
            "ingested": 1,
            "chunks_created": 1,
            "skipped": 0,
            "errors": 0,
            "latency_ms": 10,
        },
    )

    response = ingest_documents(
        "http://localhost:8080",
        "news_radar",
        [{"id": "doc-1", "title": "Test", "body": "Body"}],
        "LaBSE",
        "skip_existing",
    )

    assert response["ingested"] == 1
    assert len(httpx_mock.get_requests()) == 1


@pytest.mark.unit
def test_ingest_documents_raises_runtime_error_on_http_failure(httpx_mock) -> None:
    httpx_mock.add_response(
        url="http://localhost:8080/api/ingest/documents",
        status_code=400,
        json={"error": "Invalid collection name"},
    )

    with pytest.raises(RuntimeError, match="Invalid collection name"):
        ingest_documents(
            "http://localhost:8080",
            "bad collection!",
            [{"id": "doc-1", "title": "Test", "body": "Body"}],
            "LaBSE",
            "skip_existing",
        )
