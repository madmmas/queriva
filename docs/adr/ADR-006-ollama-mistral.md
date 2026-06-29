# ADR-006 — Local LLM: Ollama + Mistral 7B

## Status
Accepted

## Context
Queriva's RAG mode requires a locally running LLM to synthesize answers from
retrieved document chunks. The LLM must run entirely on the user's hardware
(no external API calls), be manageable via Docker, support a simple REST API,
and produce coherent multi-language output for the News Radar use case
(Bangla-sourced articles, English-language answers).

## Decision
Use **Ollama** as the local LLM runtime with **Mistral 7B** as the default model.

## Alternatives Considered

| Option | Reason rejected |
|---|---|
| **vLLM** | Higher throughput (continuous batching) but requires NVIDIA GPU and CUDA. Queriva must run on CPU-only hardware for broadest compatibility. |
| **llama.cpp** | Excellent CPU inference via GGUF quantization. But no built-in model management, no Docker image with REST API — requires custom wrapper. Ollama is essentially llama.cpp with excellent DX on top. |
| **LM Studio** | GUI-first, no Docker support, not scriptable. |
| **Hugging Face TGI** | Production-grade serving but heavyweight setup, GPU-preferred, overkill for local RAG synthesis. |
| **Llamafile** | Interesting single-binary approach but immature Docker support. |

## Ollama Rationale
- Single Docker image (`ollama/ollama`) with model management built in
- Model pull: `ollama pull mistral` — no custom scripts needed
- REST API (`POST /api/generate`) is simple and stable — matches SPEC §10 usage
- Supports CPU inference with quantized models (Mistral 7B Q4 ~4GB RAM)
- Active development, broad model support (Llama 3, Phi-3, Gemma as alternatives)

## Mistral 7B Rationale
- Strong multilingual capability — handles Bangla-sourced article summaries in English
- 4-bit quantized GGUF fits in ~4GB RAM — runs on consumer hardware without GPU
- Instruction-following quality sufficient for the constrained RAG prompt (§10)
  which explicitly bounds the LLM to provided article text
- 7B parameter sweet spot: coherent enough for synthesis, fast enough for
  interactive use (~2–5s on CPU per SPEC §15)

## Configurable Alternatives
Users can substitute any Ollama-compatible model via `OLLAMA_MODEL` env var:
- `llama3` — stronger instruction following, larger (8B)
- `phi3` — Microsoft's 3.8B model, faster on CPU
- `gemma` — Google's 7B, good multilingual

## Consequences

**Makes easier:**
- `docker compose up` pulls and serves the model without user intervention
- Model swap is a single env var change — no code changes
- CPU-only operation means Queriva works on any developer laptop

**Makes harder:**
- First startup requires model download (~4GB for Mistral Q4) — document prominently
- RAG synthesis latency is ~2–5s on CPU — acceptable but not real-time
- Ollama does not support streaming in early versions — `LLMSynthesisService`
  must await the full completion

## References
- SPEC.md §10 (LLM synthesis prompt), §12 (Docker Compose)
- Issue #16 (Ollama Docker setup), #17 (LLMSynthesisService)
- ADR-004 (Spring Boot — the layer that calls Ollama)
