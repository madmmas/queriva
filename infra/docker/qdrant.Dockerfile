# Extends the official Qdrant image with curl for Docker Compose healthchecks.
# The upstream image is minimal and does not ship curl (qdrant/qdrant#3491).
FROM qdrant/qdrant:latest

RUN apt-get update -yq \
    && apt-get install -yqq --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
