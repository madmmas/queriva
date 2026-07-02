#!/usr/bin/env python3
"""Validates fixtures/news_radar_dhaka_floods.json against SPEC §14 source fields."""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

FIXTURE_PATH = Path("fixtures/news_radar_dhaka_floods.json")
EXPECTED_DOCUMENT_COUNT = 8
EXPECTED_BANGLA_COUNT = 4
EXPECTED_ENGLISH_COUNT = 4

REQUIRED_SOURCE_FIELDS = (
    "id",
    "title",
    "body",
    "source",
    "language",
    "published_at",
    "category",
    "url",
)


def validate_fixture(fixture_path: Path) -> list[str]:
    """Returns a list of validation errors; empty when the fixture is valid."""
    errors: list[str] = []

    if not fixture_path.is_file():
        return [f"Fixture file not found: {fixture_path}"]

    try:
        payload = json.loads(fixture_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        return [f"Invalid JSON in '{fixture_path}': {error}"]

    if not isinstance(payload, list):
        return [f"Fixture '{fixture_path}' must contain a JSON array of documents."]

    if len(payload) != EXPECTED_DOCUMENT_COUNT:
        errors.append(
            f"Expected {EXPECTED_DOCUMENT_COUNT} documents but found {len(payload)}."
        )

    bangla_count = 0
    english_count = 0

    for index, document in enumerate(payload):
        if not isinstance(document, dict):
            errors.append(f"Document at index {index} must be a JSON object.")
            continue

        for field_name in REQUIRED_SOURCE_FIELDS:
            value = document.get(field_name)
            if value is None or (isinstance(value, str) and not value.strip()):
                errors.append(
                    f"Document at index {index} is missing required field '{field_name}'."
                )

        language = str(document.get("language", "")).strip()
        if language == "bn":
            bangla_count += 1
        elif language == "en":
            english_count += 1

    if bangla_count != EXPECTED_BANGLA_COUNT:
        errors.append(
            f"Expected {EXPECTED_BANGLA_COUNT} Bangla (bn) articles but found {bangla_count}."
        )

    if english_count != EXPECTED_ENGLISH_COUNT:
        errors.append(
            f"Expected {EXPECTED_ENGLISH_COUNT} English (en) articles but found {english_count}."
        )

    return errors


def main() -> int:
    """Validates the demo fixture and prints actionable errors on failure."""
    fixture_path = Path(sys.argv[1]) if len(sys.argv) > 1 else FIXTURE_PATH
    errors = validate_fixture(fixture_path)

    if errors:
        print(f"ERROR: fixture validation failed for '{fixture_path}':", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1

    print(
        f"Fixture OK: {EXPECTED_DOCUMENT_COUNT} documents "
        f"({EXPECTED_BANGLA_COUNT} bn + {EXPECTED_ENGLISH_COUNT} en) with SPEC §14 source fields."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
