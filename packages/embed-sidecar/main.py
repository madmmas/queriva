"""Queriva embed sidecar — FastAPI service for sentence embeddings."""

import logging

from fastapi import FastAPI

from models import HealthResponse

logger = logging.getLogger(__name__)

app = FastAPI(
    title="Queriva Embed Sidecar",
    description="Embedding generation via sentence-transformers",
    version="0.1.0",
)


@app.get("/api/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    """Returns sidecar health and list of loaded models."""
    return HealthResponse(status="ok", models_loaded=[])
