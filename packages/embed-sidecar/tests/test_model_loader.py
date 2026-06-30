"""Unit tests for model_loader module."""

from unittest.mock import MagicMock, patch

import numpy as np
import pytest

from model_loader import (
    ModelLoader,
    UnsupportedModelError,
    _to_float_list,
    expected_dimensions,
)


@pytest.mark.unit
def test_expected_dimensions_raises_for_unknown_model() -> None:
    with pytest.raises(UnsupportedModelError, match="Unknown model"):
        expected_dimensions("unknown-model")


@pytest.mark.unit
def test_to_float_list_converts_numpy_array() -> None:
    vector = np.array([0.1, 0.2, 0.3], dtype=np.float32)

    result = _to_float_list(vector)

    assert len(result) == 3
    assert all(isinstance(value, float) for value in result)
    assert result == pytest.approx([0.1, 0.2, 0.3])


@pytest.mark.unit
def test_to_float_list_converts_plain_list() -> None:
    result = _to_float_list([1, 2.5, 3])

    assert result == [1.0, 2.5, 3.0]


@pytest.mark.unit
@patch("model_loader.SentenceTransformer")
def test_list_loaded_models_returns_sorted_names(
    mock_sentence_transformer: MagicMock,
) -> None:
    mock_model = MagicMock()
    mock_model.encode.return_value = np.zeros(768, dtype=np.float32)
    mock_sentence_transformer.return_value = mock_model

    loader = ModelLoader()
    loader.embed("hello", "LaBSE")
    loader.embed("world", "all-MiniLM-L6-v2")

    assert loader.list_loaded_models() == ["LaBSE", "all-MiniLM-L6-v2"]
