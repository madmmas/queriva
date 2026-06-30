"""Queriva embed sidecar — FastAPI service for sentence embeddings."""

import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from model_loader import ModelLoadError, UnsupportedModelError, expected_dimensions, get_loader
from models import EmbedRequest, EmbedResponse, HealthResponse

logger = logging.getLogger(__name__)

app = FastAPI(
    title="Queriva Embed Sidecar",
    description="Embedding generation via sentence-transformers",
    version="0.1.0",
)


@app.exception_handler(UnsupportedModelError)
async def unsupported_model_handler(_request: Request, exc: UnsupportedModelError) -> JSONResponse:
    """Maps unsupported model names to HTTP 503."""
    return JSONResponse(status_code=503, content={"detail": str(exc)})


@app.exception_handler(ModelLoadError)
async def model_load_handler(_request: Request, exc: ModelLoadError) -> JSONResponse:
    """Maps model load failures to HTTP 503."""
    return JSONResponse(status_code=503, content={"detail": str(exc)})


@app.get("/api/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """Returns sidecar health and list of loaded models."""
    loader = get_loader()
    return HealthResponse(status="ok", models_loaded=loader.list_loaded_models())


# SPEC.md §6 — POST /api/embed
@app.post("/api/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest) -> EmbedResponse:
    """Embeds text with the requested model, loading the model lazily on first use."""
    loader = get_loader()
    vector = loader.embed(req.text, req.model)
    dimensions = expected_dimensions(req.model)

    if len(vector) != dimensions:
        raise HTTPException(
            status_code=500,
            detail=(
                f"Model '{req.model}' returned {len(vector)} dimensions, "
                f"expected {dimensions}. Verify the model configuration."
            ),
        )

    return EmbedResponse(vector=vector, dimensions=dimensions)
