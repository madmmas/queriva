"""Queriva batch ingestion CLI — entry point (SPEC §7.3)."""

import argparse
import logging
import sys
from typing import Any

from api_client import ingest_documents
from constants import (
    DEFAULT_API_URL,
    DEFAULT_MODEL,
    DEFAULT_UPSERT_MODE,
    STDIN_SOURCE,
)
from loaders import csv_loader, file_loader, json_loader, jsonl_loader, url_loader
from map_parser import parse_column_map

logger = logging.getLogger(__name__)

SUPPORTED_FORMATS = ("json", "csv", "files", "urls", "jsonl")
SUPPORTED_UPSERT_MODES = ("skip_existing", "overwrite", "error_on_conflict")


def build_parser() -> argparse.ArgumentParser:
    """Builds the CLI argument parser with all ingest flags."""
    parser = argparse.ArgumentParser(
        prog="queriva_ingest",
        description="Batch-ingest documents into a Queriva collection via POST /api/ingest/documents.",
    )
    parser.add_argument(
        "--api",
        dest="api_url",
        default=DEFAULT_API_URL,
        help=f"Queriva API base URL (default: {DEFAULT_API_URL})",
    )
    parser.add_argument(
        "--collection",
        required=True,
        help="Target Qdrant collection name (alphanumeric and underscore, max 64 chars)",
    )
    parser.add_argument(
        "--format",
        required=True,
        choices=list(SUPPORTED_FORMATS),
        help="Input format: json, csv, files, urls, or jsonl",
    )
    parser.add_argument(
        "--source",
        required=True,
        help="Path to input data, or '-' for jsonl on stdin",
    )
    parser.add_argument(
        "--map",
        nargs="*",
        default=[],
        metavar="FIELD=COLUMN",
        help="CSV column mapping, e.g. title=headline body=content source=outlet",
    )
    parser.add_argument(
        "--model",
        default=DEFAULT_MODEL,
        help=f"Embedding model name sent to the API (default: {DEFAULT_MODEL})",
    )
    parser.add_argument(
        "--upsert-mode",
        choices=list(SUPPORTED_UPSERT_MODES),
        default=DEFAULT_UPSERT_MODE,
        help=f"Upsert mode for duplicate document ids (default: {DEFAULT_UPSERT_MODE})",
    )
    parser.add_argument(
        "--no-chunking",
        action="store_true",
        help="Disable server-side chunking for this ingest request",
    )
    return parser


def load_documents(args: argparse.Namespace) -> list[dict[str, Any]]:
    """Dispatches to the loader matching --format."""
    if args.format == "json":
        return json_loader.load(args.source)

    if args.format == "jsonl":
        return jsonl_loader.load(args.source)

    if args.format == "csv":
        column_map = parse_column_map(args.map) if args.map else None
        return csv_loader.load(args.source, column_map)

    if args.format == "files":
        return file_loader.load(args.source)

    if args.format == "urls":
        return url_loader.load_from_file(args.source)

    raise ValueError(f"Unsupported format '{args.format}'.")


def main() -> int:
    """Parses CLI arguments, loads documents, and posts them to the ingest API."""
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    parser = build_parser()

    try:
        args = parser.parse_args()
        documents = load_documents(args)
        response = ingest_documents(
            api_url=args.api_url,
            collection=args.collection,
            documents=documents,
            model=args.model,
            upsert_mode=args.upsert_mode,
            chunking_enabled=not args.no_chunking,
        )
    except (ValueError, RuntimeError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1

    logger.info(
        "Ingest complete for collection '%s': ingested=%s chunks_created=%s skipped=%s errors=%s latency_ms=%s",
        response.get("collection", args.collection if "args" in locals() else "?"),
        response.get("ingested"),
        response.get("chunks_created"),
        response.get("skipped"),
        response.get("errors"),
        response.get("latency_ms"),
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
