"""Unit tests for map_parser."""

import pytest

from map_parser import parse_column_map


@pytest.mark.unit
def test_parse_column_map_parses_field_column_pairs() -> None:
    column_map = parse_column_map(["title=headline", "body=content", "source=outlet"])

    assert column_map == {
        "title": "headline",
        "body": "content",
        "source": "outlet",
    }


@pytest.mark.unit
def test_parse_column_map_rejects_invalid_tokens() -> None:
    with pytest.raises(ValueError, match="Invalid --map entry"):
        parse_column_map(["title-headline"])
