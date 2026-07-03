// SPEC.md §6 — API contract types (code-quality.mdc D2)

/** Search mode for POST /api/search. */
export type SearchMode = 'search' | 'rag';

/** Dependency connectivity status from GET /api/health. */
export type DependencyStatus = 'connected' | 'disconnected';

/** Overall health status from GET /api/health. */
export type HealthStatus = 'ok' | 'degraded';

/** Document upsert behaviour for POST /api/ingest/documents. */
export type UpsertMode = 'skip_existing' | 'overwrite' | 'error_on_conflict';

/** Optional Qdrant payload filters for vector search. */
export interface SearchFilters {
  language?: string | null;
  date_from?: string | null;
  date_to?: string | null;
  category?: string | null;
}

/** Request body for POST /api/search. */
export interface SearchRequest {
  query: string;
  collection: string;
  top_k?: number;
  min_score?: number;
  mode?: SearchMode;
  filters?: SearchFilters | null;
}

/** One ranked search result (SPEC §6). */
export interface SearchResult {
  id: string;
  score: number;
  title: string;
  snippet: string;
  source: string;
  language: string;
  published_at: string;
  url: string;
}

/** Latency breakdown for POST /api/search. */
export interface SearchLatencyMs {
  embed: number;
  search: number;
  synthesis?: number | null;
  total: number;
}

/** Response body for POST /api/search. */
export interface SearchResponse {
  query: string;
  mode: SearchMode;
  summary?: string | null;
  results: SearchResult[];
  latency_ms: SearchLatencyMs;
}

/** Response body for GET /api/health. */
export interface HealthResponse {
  status: HealthStatus;
  qdrant: DependencyStatus;
  ollama: DependencyStatus;
  embed_sidecar: DependencyStatus;
}

/** One source document in POST /api/ingest/documents. */
export interface IngestDocument {
  id: string;
  title: string;
  body: string;
  source?: string | null;
  language?: string | null;
  published_at?: string | null;
  category?: string | null;
  url?: string | null;
  cluster_id?: string | null;
}

/** Chunking configuration for POST /api/ingest/documents. */
export interface ChunkingOptions {
  enabled?: boolean;
  chunk_size?: number;
  overlap?: number;
}

/** Request body for POST /api/ingest/documents. */
export interface IngestRequest {
  collection: string;
  model?: string;
  documents: IngestDocument[];
  chunking?: ChunkingOptions;
  upsert_mode?: UpsertMode;
}

/** Response body for POST /api/ingest/documents. */
export interface IngestResponse {
  collection: string;
  ingested: number;
  chunks_created: number;
  skipped: number;
  errors: number;
  latency_ms: number;
}

/** Request body for POST /api/ingest/collection. */
export interface CreateCollectionRequest {
  collection: string;
  vector_size?: number;
  distance?: string;
  recreate_if_exists?: boolean;
}

/** Summary of one Qdrant collection. */
export interface CollectionSummary {
  name: string;
  vector_size: number;
  distance: string;
  points_count: number;
}

/** Response body for GET /api/ingest/collections. */
export interface CollectionListResponse {
  collections: CollectionSummary[];
}

/** Request body for POST /api/embed (embed-sidecar). */
export interface EmbedRequest {
  text: string;
  model?: string;
}

/** Response body for POST /api/embed (embed-sidecar). */
export interface EmbedResponse {
  vector: number[];
  dimensions: number;
}
