"""HTTP client for Queriva ingest API (SPEC §6, §7.2)."""

from typing import Any

import httpx

from constants import (
    DEFAULT_CHUNK_SIZE,
    DEFAULT_OVERLAP,
    HTTP_CONNECT_TIMEOUT_SECONDS,
    HTTP_READ_TIMEOUT_SECONDS,
)

INGEST_DOCUMENTS_PATH = "/api/ingest/documents"


def ingest_documents(
        api_url: str,
        collection: str,
        documents: list[dict[str, Any]],
        model: str,
        upsert_mode: str,
        chunking_enabled: bool = True,
) -> dict[str, Any]:
    """Posts documents to POST /api/ingest/documents and returns the ingest report."""
    request_body = {
        "collection": collection,
        "model": model,
        "documents": documents,
        "chunking": {
            "enabled": chunking_enabled,
            "chunk_size": DEFAULT_CHUNK_SIZE,
            "overlap": DEFAULT_OVERLAP,
        },
        "upsert_mode": upsert_mode,
    }

    timeout = httpx.Timeout(
        connect=HTTP_CONNECT_TIMEOUT_SECONDS,
        read=HTTP_READ_TIMEOUT_SECONDS,
        write=HTTP_READ_TIMEOUT_SECONDS,
        pool=HTTP_CONNECT_TIMEOUT_SECONDS,
    )

    try:
        with httpx.Client(timeout=timeout) as client:
            response = client.post(
                f"{api_url.rstrip('/')}{INGEST_DOCUMENTS_PATH}",
                json=request_body,
            )
            response.raise_for_status()
            return response.json()
    except httpx.HTTPStatusError as error:
        detail = _extract_error_detail(error.response)
        raise RuntimeError(
            f"Ingest API returned {error.response.status_code} for collection '{collection}'. {detail}"
        ) from error
    except httpx.RequestError as error:
        raise RuntimeError(
            f"Failed to reach Queriva API at '{api_url}'. "
            f"Verify the API is running and QUERIVA_API_URL is correct. Cause: {error}"
        ) from error


def _extract_error_detail(response: httpx.Response) -> str:
    """Extracts an actionable error message from an API error response."""
    try:
        payload = response.json()
        if isinstance(payload, dict) and "error" in payload:
            return str(payload["error"])
    except ValueError:
        pass

    return response.text.strip() or "No error detail returned."
