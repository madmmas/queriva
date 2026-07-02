"""URL loader with SSRF prevention (SPEC §7.3, code-quality.mdc E4)."""

import hashlib
import ipaddress
import logging
import socket
import urllib.parse
from typing import Any

import httpx
import trafilatura

from constants import HTTP_CONNECT_TIMEOUT_SECONDS, HTTP_READ_TIMEOUT_SECONDS
from document_utils import validate_documents

logger = logging.getLogger(__name__)

HTTP_TIMEOUT = httpx.Timeout(
    connect=HTTP_CONNECT_TIMEOUT_SECONDS,
    read=HTTP_READ_TIMEOUT_SECONDS,
    write=HTTP_READ_TIMEOUT_SECONDS,
    pool=HTTP_CONNECT_TIMEOUT_SECONDS,
)


def is_safe_url(url: str) -> bool:
    """Returns True when the URL does not resolve to a private or loopback address."""
    hostname = urllib.parse.urlparse(url).hostname
    if hostname is None:
        return False

    try:
        ip_address = ipaddress.ip_address(socket.gethostbyname(hostname))
        return not (ip_address.is_private or ip_address.is_loopback or ip_address.is_link_local)
    except (socket.gaierror, ValueError):
        return False


def load(urls: list[str]) -> list[dict[str, Any]]:
    """Fetches and extracts text from public URLs."""
    documents: list[dict[str, Any]] = []

    for url in urls:
        normalized_url = url.strip()
        if not normalized_url:
            continue

        if not is_safe_url(normalized_url):
            raise ValueError(
                f"URL '{normalized_url}' resolves to a private or loopback address and was blocked."
            )

        document = _fetch_url_document(normalized_url)
        if document is not None:
            documents.append(document)

    return validate_documents(documents)


def load_from_file(path: str) -> list[dict[str, Any]]:
    """Loads URLs from a text file with one URL per line."""
    try:
        with open(path, encoding="utf-8") as url_file:
            urls = [line.strip() for line in url_file if line.strip()]
    except OSError as error:
        raise ValueError(f"URL list file not readable: {path}") from error

    if not urls:
        raise ValueError(f"URL list file '{path}' contains no URLs.")

    return load(urls)


def _fetch_url_document(url: str) -> dict[str, Any] | None:
    """Fetches one URL and extracts a document payload."""
    try:
        with httpx.Client(timeout=HTTP_TIMEOUT, follow_redirects=True) as client:
            response = client.get(url)
            response.raise_for_status()
            html = response.text
    except httpx.HTTPError as error:
        raise RuntimeError(f"Failed to fetch URL '{url}': {error}") from error

    extracted = trafilatura.extract(html, url=url, include_comments=False, include_tables=False)
    if extracted is None or not extracted.strip():
        logger.warning("No extractable text found for URL '%s' — skipping.", url)
        return None

    parsed = urllib.parse.urlparse(url)
    title = parsed.netloc or url

    return {
        "id": hashlib.sha256(url.encode("utf-8")).hexdigest()[:16],
        "title": title,
        "body": extracted.strip(),
        "source": parsed.netloc,
        "language": "en",
        "url": url,
    }
