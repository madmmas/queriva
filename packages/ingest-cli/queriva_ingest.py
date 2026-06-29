"""Queriva batch ingestion CLI — entry point."""

import argparse
import logging
import sys

DEFAULT_API_URL = "http://localhost:8080"
DEFAULT_COLLECTION = "news_radar"

logger = logging.getLogger(__name__)


def build_parser() -> argparse.ArgumentParser:
    """Builds the CLI argument parser with all ingest flags."""
    parser = argparse.ArgumentParser(
        prog="queriva_ingest",
        description="Batch-ingest documents into a Queriva collection.",
    )
    parser.add_argument(
        "--collection",
        required=True,
        help="Target Qdrant collection name",
    )
    parser.add_argument(
        "--format",
        required=True,
        choices=["json", "csv", "files", "urls", "jsonl"],
        help="Input format to load",
    )
    parser.add_argument(
        "--api-url",
        default=DEFAULT_API_URL,
        help=f"Queriva API base URL (default: {DEFAULT_API_URL})",
    )
    parser.add_argument(
        "--source",
        required=True,
        help="Path or URL to input data",
    )
    return parser


def main() -> int:
    """Parses CLI arguments and dispatches to the appropriate loader."""
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    parser = build_parser()
    args = parser.parse_args()
    logger.info(
        "Ingest CLI scaffold — format=%s collection=%s (implemented in issue #9)",
        args.format,
        args.collection,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
