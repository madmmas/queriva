"""Unit tests for api_client."""

import httpx
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
@pytest.mark.parametrize(
    "status_code,error_detail",
    [
        (400, "Invalid collection name"),
        (409, "Document already exists"),
        (500, "Internal server error"),
    ],
)
def test_ingest_documents_raises_runtime_error_on_http_failure(
        httpx_mock,
        status_code: int,
        error_detail: str,
) -> None:
    httpx_mock.add_response(
        url="http://localhost:8080/api/ingest/documents",
        status_code=status_code,
        json={"error": error_detail},
    )

    with pytest.raises(RuntimeError, match=error_detail):
        ingest_documents(
            "http://localhost:8080",
            "news_radar",
            [{"id": "doc-1", "title": "Test", "body": "Body"}],
            "LaBSE",
            "skip_existing",
        )


@pytest.mark.unit
def test_ingest_documents_raises_runtime_error_when_api_is_unreachable(httpx_mock) -> None:
    api_url = "http://127.0.0.1:9"
    httpx_mock.add_exception(
        httpx.ConnectError("connection refused"),
        url=f"{api_url}/api/ingest/documents",
    )

    with pytest.raises(RuntimeError, match="Failed to reach Queriva API"):
        ingest_documents(
            api_url,
            "news_radar",
            [{"id": "doc-1", "title": "Test", "body": "Body"}],
            "LaBSE",
            "skip_existing",
        )


@pytest.mark.unit
def test_ingest_documents_uses_plain_text_error_when_json_body_is_missing(httpx_mock) -> None:
    httpx_mock.add_response(
        url="http://localhost:8080/api/ingest/documents",
        status_code=500,
        text="upstream timeout",
    )

    with pytest.raises(RuntimeError, match="upstream timeout"):
        ingest_documents(
            "http://localhost:8080",
            "news_radar",
            [{"id": "doc-1", "title": "Test", "body": "Body"}],
            "LaBSE",
            "skip_existing",
        )
