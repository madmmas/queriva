"""Unit tests for ingest CLI loaders."""

import json
from io import StringIO
from pathlib import Path

import pytest

from loaders import csv_loader, file_loader, json_loader, jsonl_loader, url_loader


@pytest.mark.unit
def test_json_loader_reads_document_array_from_file(tmp_path: Path) -> None:
    fixture = tmp_path / "articles.json"
    fixture.write_text(
        json.dumps([
            {"id": "doc-1", "title": "Floods", "body": "Heavy rains in Dhaka."},
        ]),
        encoding="utf-8",
    )

    documents = json_loader.load(str(fixture))

    assert len(documents) == 1
    assert documents[0]["id"] == "doc-1"


@pytest.mark.unit
def test_jsonl_loader_reads_documents_from_stream() -> None:
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
def test_file_loader_rejects_path_traversal(tmp_path: Path) -> None:
    with pytest.raises(ValueError, match="Path traversal detected"):
        file_loader.safe_path(str(tmp_path), "../outside.txt")


@pytest.mark.unit
def test_url_loader_blocks_private_ip_addresses(monkeypatch: pytest.MonkeyPatch) -> None:
    def resolve_public(_hostname: str) -> str:
        return "93.184.216.34"

    def resolve_private(_hostname: str) -> str:
        return "127.0.0.1"

    monkeypatch.setattr(url_loader.socket, "gethostbyname", resolve_public)
    assert url_loader.is_safe_url("https://example.com") is True

    monkeypatch.setattr(url_loader.socket, "gethostbyname", resolve_private)
    assert url_loader.is_safe_url("http://127.0.0.1/article") is False
    assert url_loader.is_safe_url("http://localhost/article") is False


@pytest.mark.unit
def test_json_loader_rejects_non_array_payload(tmp_path: Path) -> None:
    fixture = tmp_path / "articles.json"
    fixture.write_text('{"id": "doc-1"}', encoding="utf-8")

    with pytest.raises(ValueError, match="must contain an array"):
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
def test_url_loader_load_from_file_reads_urls(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
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
