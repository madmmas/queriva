"""Lazy-loaded sentence-transformer model cache."""

import logging
from dataclasses import dataclass
from typing import Final

import numpy as np
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class ModelSpec:
    """HuggingFace model id and expected embedding dimensions."""

    huggingface_id: str
    dimensions: int


# SPEC §9 — supported models (ADR-002)
SUPPORTED_MODELS: Final[dict[str, ModelSpec]] = {
    "LaBSE": ModelSpec(huggingface_id="sentence-transformers/LaBSE", dimensions=768),
    "all-MiniLM-L6-v2": ModelSpec(
        huggingface_id="sentence-transformers/all-MiniLM-L6-v2",
        dimensions=384,
    ),
    "paraphrase-multilingual-mpnet-base-v2": ModelSpec(
        huggingface_id="sentence-transformers/paraphrase-multilingual-mpnet-base-v2",
        dimensions=768,
    ),
}


class UnsupportedModelError(Exception):
    """Raised when the requested model name is not in SUPPORTED_MODELS."""


class ModelLoadError(Exception):
    """Raised when sentence-transformers fails to load a supported model."""


class ModelLoader:
    """Loads and caches SentenceTransformer models on first use per model name."""

    def __init__(self) -> None:
        self._cache: dict[str, SentenceTransformer] = {}

    def list_loaded_models(self) -> list[str]:
        """Returns model names currently held in the in-memory cache."""
        return sorted(self._cache.keys())

    def clear_cache(self) -> None:
        """Clears all cached models from memory."""
        self._cache.clear()

    def embed(self, text: str, model_name: str) -> list[float]:
        """Embeds text with the named model, loading the model lazily if needed."""
        transformer = self._get_or_load(model_name)
        vector = transformer.encode(text)
        return _to_float_list(vector)

    def _get_or_load(self, model_name: str) -> SentenceTransformer:
        if model_name not in SUPPORTED_MODELS:
            supported = ", ".join(sorted(SUPPORTED_MODELS))
            raise UnsupportedModelError(
                f"Model '{model_name}' is not supported. Use one of: {supported}."
            )

        if model_name not in self._cache:
            spec = SUPPORTED_MODELS[model_name]
            try:
                transformer = SentenceTransformer(spec.huggingface_id)
            except OSError as error:
                raise ModelLoadError(
                    f"Model '{model_name}' could not be loaded from "
                    f"'{spec.huggingface_id}'. Verify network access for the first "
                    f"download and that sentence-transformers is installed. Cause: {error}"
                ) from error

            self._cache[model_name] = transformer
            logger.info(
                "Loaded model '%s' (%d-dim)",
                model_name,
                spec.dimensions,
            )

        return self._cache[model_name]


def expected_dimensions(model_name: str) -> int:
    """Returns the expected vector dimensions for a supported model name."""
    if model_name not in SUPPORTED_MODELS:
        raise UnsupportedModelError(f"Unknown model: {model_name}")
    return SUPPORTED_MODELS[model_name].dimensions


def _to_float_list(vector: np.ndarray | list[float]) -> list[float]:
    """Converts an embedding array to a plain list of floats."""
    if isinstance(vector, list):
        return [float(value) for value in vector]
    return vector.astype(float).tolist()


# Module-level singleton used by FastAPI routes
_loader = ModelLoader()


def get_loader() -> ModelLoader:
    """Returns the shared model loader instance."""
    return _loader
