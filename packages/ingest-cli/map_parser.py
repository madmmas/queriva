"""Parses --map column mapping arguments for CSV ingest."""


def parse_column_map(map_tokens: list[str]) -> dict[str, str]:
    """Parses tokens like title=headline body=content into a column map."""
    column_map: dict[str, str] = {}

    for token in map_tokens:
        if "=" not in token:
            raise ValueError(
                f"Invalid --map entry '{token}'. Expected format: document_field=csv_column."
            )

        document_field, csv_column = token.split("=", 1)
        document_field = document_field.strip()
        csv_column = csv_column.strip()

        if not document_field or not csv_column:
            raise ValueError(
                f"Invalid --map entry '{token}'. Both document field and CSV column are required."
            )

        column_map[document_field] = csv_column

    return column_map
