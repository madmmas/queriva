"""Pydantic request/response models for the embed sidecar API."""

from pydantic import BaseModel, Field

DEFAULT_MODEL = "LaBSE"


class EmbedRequest(BaseModel):
    """Request body for POST /api/embed."""

    text: str = Field(..., min_length=1)
    model: str = DEFAULT_MODEL


class EmbedResponse(BaseModel):
    """Response body for POST /api/embed."""

    vector: list[float]
    dimensions: int


class HealthResponse(BaseModel):
    """Response body for GET /api/health."""

    status: str
    models_loaded: list[str]
