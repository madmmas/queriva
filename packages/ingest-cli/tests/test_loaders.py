"""Unit tests for ingest CLI loaders."""

import json
from io import StringIO
from pathlib import Path
from typing import Any

import httpx
import pytest

from loaders import csv_loader, file_loader, json_loader, jsonl_loader, url_loader


@pytest.mark.unit
@pytest.mark.parametrize(
    "format_name,loader_callable,fixture_key",
    [
        ("json", json_loader.load, "json"),
        ("csv", csv_loader.load, "csv"),
        ("jsonl", jsonl_loader.load, "jsonl"),
        ("files", file_loader.load, "files"),
    ],
)
def test_loaders_return_valid_documents_for_each_format(
        format_name: str,
        loader_callable,
        fixture_key: str,
        loader_fixtures: dict[str, Any],
) -> None:
    fixture = loader_fixtures[fixture_key]
    documents = loader_callable(fixture["path"])

    assert len(documents) == fixture["expected_count"]
    assert documents[0]["id"]
    assert documents[0]["title"]


@pytest.mark.unit
def test_jsonl_loader_reads_documents_from_stdin_stream() -> None:
    stream = StringIO(
        '{"id": "doc-1", "title": "One", "body": "A"}\n'
        '{"id": "doc-2", "title": "Two", "body": "B"}\n'
    )

    documents = jsonl_loader.load("-", input_stream=stream)

    assert len(documents) == 2
    assert documents[0]["title"] == "One"


@pytest.mark.unit
def test_csv_loader_applies_column_mapping(tmp_path: Path) -> None:
    fixture = tmp_path / "articles.csv"
    fixture.write_text(
        "headline,content,outlet\n"
        "Floods in Dhaka,Heavy rains,prothomalo.com\n",
        encoding="utf-8",
    )

    documents = csv_loader.load(
        str(fixture),
        {"id": "headline", "title": "headline", "body": "content", "source": "outlet"},
    )

    assert documents[0]["title"] == "Floods in Dhaka"
    assert documents[0]["body"] == "Heavy rains"
    assert documents[0]["source"] == "prothomalo.com"


@pytest.mark.unit
def test_file_loader_reads_txt_md_and_pdf_recursively(tmp_path: Path) -> None:
    docs_dir = tmp_path / "documents"
    docs_dir.mkdir()
    (docs_dir / "note.txt").write_text("Plain text body.", encoding="utf-8")
    nested = docs_dir / "nested"
    nested.mkdir()
    (nested / "memo.md").write_text("Markdown body.", encoding="utf-8")

    documents = file_loader.load(str(docs_dir))

    assert len(documents) == 2
    assert {document["title"] for document in documents} == {"note", "memo"}


@pytest.mark.unit
def test_file_loader_rejects_path_traversal_with_etc_passwd(tmp_path: Path) -> None:
    with pytest.raises(ValueError, match="Path traversal detected"):
        file_loader.safe_path(str(tmp_path), "../../../etc/passwd")


@pytest.mark.unit
def test_file_loader_rejects_relative_escape(tmp_path: Path) -> None:
    with pytest.raises(ValueError, match="Path traversal detected"):
        file_loader.safe_path(str(tmp_path), "../outside.txt")


@pytest.mark.unit
@pytest.mark.parametrize(
    "resolved_ip,expected_safe",
    [
        ("93.184.216.34", True),
        ("127.0.0.1", False),
        ("10.0.0.1", False),
        ("192.168.1.1", False),
        ("172.16.0.1", False),
    ],
)
def test_url_loader_blocks_private_and_loopback_addresses(
        monkeypatch: pytest.MonkeyPatch,
        resolved_ip: str,
        expected_safe: bool,
) -> None:
    monkeypatch.setattr(url_loader.socket, "gethostbyname", lambda _hostname: resolved_ip)

    assert url_loader.is_safe_url("https://example.com/article") is expected_safe


@pytest.mark.unit
def test_url_loader_rejects_urls_without_hostname() -> None:
    assert url_loader.is_safe_url("not-a-valid-url") is False


@pytest.mark.unit
def test_url_loader_rejects_unresolvable_hostname(monkeypatch: pytest.MonkeyPatch) -> None:
    def raise_gaierror(_hostname: str) -> str:
        raise url_loader.socket.gaierror("Name or service not known")

    monkeypatch.setattr(url_loader.socket, "gethostbyname", raise_gaierror)

    assert url_loader.is_safe_url("https://does-not-exist.invalid") is False


@pytest.mark.unit
def test_json_loader_rejects_non_array_payload(tmp_path: Path) -> None:
    fixture = tmp_path / "articles.json"
    fixture.write_text('{"id": "doc-1"}', encoding="utf-8")

    with pytest.raises(ValueError, match="must contain an array"):
        json_loader.load(str(fixture))


@pytest.mark.unit
def test_json_loader_rejects_missing_file() -> None:
    with pytest.raises(ValueError, match="JSON source file not found"):
        json_loader.load("/nonexistent/articles.json")


@pytest.mark.unit
def test_json_loader_rejects_invalid_json(tmp_path: Path) -> None:
    fixture = tmp_path / "broken.json"
    fixture.write_text("{not valid json", encoding="utf-8")

    with pytest.raises(ValueError, match="Invalid JSON"):
        json_loader.load(str(fixture))


@pytest.mark.unit
def test_json_loader_rejects_non_object_entries(tmp_path: Path) -> None:
    fixture = tmp_path / "articles.json"
    fixture.write_text('["not", "objects"]', encoding="utf-8")

    with pytest.raises(ValueError, match="must be a JSON object"):
        json_loader.load(str(fixture))


@pytest.mark.unit
def test_csv_loader_uses_default_column_map(tmp_path: Path) -> None:
    fixture = tmp_path / "articles.csv"
    fixture.write_text(
        "id,title,body\n"
        "doc-1,Floods,Heavy rains\n",
        encoding="utf-8",
    )

    documents = csv_loader.load(str(fixture))

    assert documents[0]["id"] == "doc-1"
    assert documents[0]["title"] == "Floods"


@pytest.mark.unit
def test_csv_loader_rejects_missing_file() -> None:
    with pytest.raises(ValueError, match="CSV source file not found"):
        csv_loader.load("/nonexistent/articles.csv")


@pytest.mark.unit
def test_jsonl_loader_skips_blank_lines() -> None:
    stream = StringIO(
        '{"id": "doc-1", "title": "One", "body": "A"}\n'
        '\n'
        '{"id": "doc-2", "title": "Two", "body": "B"}\n'
    )

    documents = jsonl_loader.load("-", input_stream=stream)

    assert len(documents) == 2


@pytest.mark.unit
def test_csv_loader_skips_empty_rows(tmp_path: Path) -> None:
    fixture = tmp_path / "articles.csv"
    fixture.write_text(
        "id,title,body\n"
        "doc-1,Floods,Heavy rains\n"
        ",,\n",
        encoding="utf-8",
    )

    documents = csv_loader.load(str(fixture))

    assert len(documents) == 1


@pytest.mark.unit
def test_csv_loader_rejects_missing_header_row(tmp_path: Path) -> None:
    fixture = tmp_path / "empty.csv"
    fixture.write_text("", encoding="utf-8")

    with pytest.raises(ValueError, match="no header row"):
        csv_loader.load(str(fixture))
    fixture = tmp_path / "empty.csv"
    fixture.write_text("", encoding="utf-8")

    with pytest.raises(ValueError, match="no header row"):
        csv_loader.load(str(fixture))


@pytest.mark.unit
def test_jsonl_loader_rejects_missing_file() -> None:
    with pytest.raises(ValueError, match="JSONL source file not found"):
        jsonl_loader.load("/nonexistent/articles.jsonl")


@pytest.mark.unit
def test_jsonl_loader_rejects_invalid_json_line() -> None:
    stream = StringIO('{"id": "doc-1", "title": "One", "body": "A"}\n{broken\n')

    with pytest.raises(ValueError, match="Invalid JSON on line 2"):
        jsonl_loader.load("-", input_stream=stream)


@pytest.mark.unit
def test_jsonl_loader_rejects_non_object_line() -> None:
    stream = StringIO('"just a string"\n')

    with pytest.raises(ValueError, match="must be a JSON object"):
        jsonl_loader.load("-", input_stream=stream)


@pytest.mark.unit
def test_file_loader_rejects_missing_directory() -> None:
    with pytest.raises(ValueError, match="File source directory not found"):
        file_loader.load("/nonexistent/documents")


@pytest.mark.unit
def test_file_loader_skips_empty_text_files(tmp_path: Path) -> None:
    docs_dir = tmp_path / "documents"
    docs_dir.mkdir()
    (docs_dir / "empty.txt").write_text("   \n", encoding="utf-8")
    (docs_dir / "note.txt").write_text("Has content.", encoding="utf-8")

    documents = file_loader.load(str(docs_dir))

    assert len(documents) == 1
    assert documents[0]["title"] == "note"


@pytest.mark.unit
def test_file_loader_reads_pdf_files(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    docs_dir = tmp_path / "documents"
    docs_dir.mkdir()
    pdf_path = docs_dir / "report.pdf"
    pdf_path.write_bytes(b"%PDF-1.4 placeholder")

    monkeypatch.setattr(file_loader, "_read_pdf_text", lambda _path: "Extracted PDF body.")

    documents = file_loader.load(str(docs_dir))

    assert len(documents) == 1
    assert documents[0]["body"] == "Extracted PDF body."


@pytest.mark.unit
def test_read_file_body_rejects_unsupported_suffix(tmp_path: Path) -> None:
    unsupported = tmp_path / "data.xyz"
    unsupported.write_text("content", encoding="utf-8")

    with pytest.raises(ValueError, match="Unsupported file type"):
        file_loader._read_file_body(unsupported)


@pytest.mark.unit
def test_read_pdf_text_reads_blank_pdf(tmp_path: Path) -> None:
    from pypdf import PdfWriter

    pdf_path = tmp_path / "blank.pdf"
    writer = PdfWriter()
    writer.add_blank_page(width=200, height=200)
    with pdf_path.open("wb") as pdf_file:
        writer.write(pdf_file)

    assert file_loader._read_pdf_text(pdf_path) == ""


@pytest.mark.unit
def test_url_loader_load_from_file_reads_urls(
        tmp_path: Path,
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    url_file = tmp_path / "urls.txt"
    url_file.write_text("https://example.com/article\n", encoding="utf-8")

    monkeypatch.setattr(url_loader, "load", lambda urls: [
        {"id": "abc", "title": "Example", "body": "Text"},
    ])

    documents = url_loader.load_from_file(str(url_file))

    assert documents[0]["title"] == "Example"


@pytest.mark.unit
def test_url_loader_rejects_private_urls_before_fetch(monkeypatch: pytest.MonkeyPatch) -> None:
    def fail_fetch(_url: str) -> None:
        raise AssertionError("fetch should not be called for blocked URLs")

    monkeypatch.setattr(url_loader, "_fetch_url_document", fail_fetch)

    with pytest.raises(ValueError, match="private or loopback"):
        url_loader.load(["http://127.0.0.1/private"])


@pytest.mark.unit
def test_url_loader_rejects_empty_url_list_file(tmp_path: Path) -> None:
    url_file = tmp_path / "urls.txt"
    url_file.write_text("\n\n", encoding="utf-8")

    with pytest.raises(ValueError, match="contains no URLs"):
        url_loader.load_from_file(str(url_file))


@pytest.mark.unit
def test_url_loader_rejects_unreadable_url_list_file() -> None:
    with pytest.raises(ValueError, match="URL list file not readable"):
        url_loader.load_from_file("/nonexistent/urls.txt")


@pytest.mark.unit
def test_url_loader_fetches_public_url_and_extracts_body(
        httpx_mock,
        resolve_public_ip,
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    httpx_mock.add_response(
        url="https://example.com/flood-article",
        text="<html><body><p>Flood coverage.</p></body></html>",
    )
    monkeypatch.setattr(
        url_loader.trafilatura,
        "extract",
        lambda _html, **kwargs: "Flood coverage.",
    )

    documents = url_loader.load(["https://example.com/flood-article"])

    assert documents[0]["title"] == "example.com"
    assert documents[0]["body"] == "Flood coverage."
    assert documents[0]["url"] == "https://example.com/flood-article"


@pytest.mark.unit
def test_url_loader_skips_urls_with_no_extractable_text(
        httpx_mock,
        resolve_public_ip,
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    httpx_mock.add_response(url="https://example.com/empty", text="<html></html>")
    monkeypatch.setattr(url_loader.trafilatura, "extract", lambda _html, **kwargs: None)

    with pytest.raises(ValueError, match="No documents loaded"):
        url_loader.load(["https://example.com/empty"])


@pytest.mark.unit
def test_url_loader_skips_blank_url_entries(
        resolve_public_ip,
        monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(
        url_loader,
        "_fetch_url_document",
        lambda url: {"id": "url-1", "title": "Fetched", "body": "Text"},
    )

    documents = url_loader.load(["", "   ", "https://example.com/article"])

    assert len(documents) == 1


@pytest.mark.unit
def test_url_loader_raises_when_fetch_fails(
        httpx_mock,
        resolve_public_ip,
) -> None:
    httpx_mock.add_exception(
        httpx.ConnectError("connection refused"),
        url="https://example.com/down",
    )

    with pytest.raises(RuntimeError, match="Failed to fetch URL"):
        url_loader.load(["https://example.com/down"])
