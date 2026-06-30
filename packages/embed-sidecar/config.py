"""Environment configuration for the embed sidecar."""

import os

# SPEC §13 — EMBED_DEFAULT_MODEL
DEFAULT_MODEL = os.getenv("DEFAULT_MODEL", os.getenv("EMBED_DEFAULT_MODEL", "LaBSE"))

# SPEC §9 — embed sidecar listens on port 8001
DEFAULT_PORT = 8001
PORT = int(os.getenv("PORT", str(DEFAULT_PORT)))

# Rule E2 — document body max 100KB; embed accepts single chunk text
MAX_EMBED_TEXT_CHARS = int(os.getenv("MAX_EMBED_TEXT_CHARS", "100000"))

# Supported model names use letters, digits, dots, underscores, and hyphens only
MODEL_NAME_PATTERN = r"^[a-zA-Z0-9][a-zA-Z0-9._-]*$"
MODEL_NAME_MAX_LENGTH = 128
