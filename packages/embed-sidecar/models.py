"""Pydantic request/response models for the embed sidecar API."""

from pydantic import BaseModel, Field

from config import (
    DEFAULT_MODEL,
    MAX_EMBED_TEXT_CHARS,
    MODEL_NAME_MAX_LENGTH,
    MODEL_NAME_PATTERN,
)


class EmbedRequest(BaseModel):
    """Request body for POST /api/embed."""

    text: str = Field(..., min_length=1, max_length=MAX_EMBED_TEXT_CHARS)
    model: str = Field(
        default=DEFAULT_MODEL,
        min_length=1,
        max_length=MODEL_NAME_MAX_LENGTH,
        pattern=MODEL_NAME_PATTERN,
    )


class EmbedResponse(BaseModel):
    """Response body for POST /api/embed."""

    vector: list[float]
    dimensions: int


class HealthResponse(BaseModel):
    """Response body for GET /api/health."""

    status: str
    models_loaded: list[str]
