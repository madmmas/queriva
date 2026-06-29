"""Verifies loader stubs raise NotImplementedError until issue #9."""

import pytest

from loaders import csv_loader, file_loader, json_loader, url_loader


@pytest.mark.unit
def test_json_loader_raises_not_implemented_when_called() -> None:
    with pytest.raises(NotImplementedError, match="issue #9"):
        json_loader.load("data.json")


@pytest.mark.unit
def test_csv_loader_raises_not_implemented_when_called() -> None:
    with pytest.raises(NotImplementedError, match="issue #9"):
        csv_loader.load("data.csv")


@pytest.mark.unit
def test_file_loader_raises_not_implemented_when_called() -> None:
    with pytest.raises(NotImplementedError, match="issue #9"):
        file_loader.load("/tmp")


@pytest.mark.unit
def test_url_loader_raises_not_implemented_when_called() -> None:
    with pytest.raises(NotImplementedError, match="issue #9"):
        url_loader.load(["https://example.com"])
