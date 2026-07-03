import { describe, expect, it } from 'vitest';
import type {
  CollectionListResponse,
  EmbedResponse,
  HealthResponse,
  IngestRequest,
  IngestResponse,
  SearchRequest,
  SearchResponse,
} from './api';

describe('api types', () => {
  it('should accept a valid search request shape from spec section six', () => {
    const request: SearchRequest = {
      query: 'floods in Dhaka last week',
      collection: 'news_radar',
      top_k: 10,
      min_score: 0.6,
      mode: 'rag',
      filters: {
        language: 'bn',
        date_from: '2026-06-01',
        date_to: '2026-06-28',
        category: 'national',
      },
    };

    expect(request.mode).toBe('rag');
  });

  it('should accept a valid search response shape from spec section six', () => {
    const response: SearchResponse = {
      query: 'floods in Dhaka last week',
      mode: 'rag',
      summary: 'Three major floods hit Dhaka between June 14–20.',
      results: [
        {
          id: 'cluster-001',
          score: 0.892,
          title: 'Buriganga river floods Dhaka low-lying areas',
          snippet: 'Heavy monsoon rains caused the Buriganga river to overflow...',
          source: 'prothomalo.com',
          language: 'bn',
          published_at: '2026-06-15T08:30:00Z',
          url: 'https://example.com/article',
        },
      ],
      latency_ms: {
        embed: 45,
        search: 23,
        synthesis: 1840,
        total: 1908,
      },
    };

    expect(response.latency_ms.synthesis).toBeGreaterThan(0);
  });

  it('should accept health ingest and embed api shapes from spec section six', () => {
    const health: HealthResponse = {
      status: 'ok',
      qdrant: 'connected',
      ollama: 'connected',
      embed_sidecar: 'connected',
    };

    const ingestRequest: IngestRequest = {
      collection: 'news_radar',
      model: 'LaBSE',
      documents: [
        {
          id: 'cluster-001',
          title: 'Buriganga river overflows',
          body: 'Heavy monsoon rains...',
          source: 'prothomalo.com',
          language: 'bn',
        },
      ],
      chunking: { enabled: true, chunk_size: 512, overlap: 64 },
      upsert_mode: 'skip_existing',
    };

    const ingestResponse: IngestResponse = {
      collection: 'news_radar',
      ingested: 8,
      chunks_created: 24,
      skipped: 0,
      errors: 0,
      latency_ms: 4821,
    };

    const collections: CollectionListResponse = {
      collections: [
        {
          name: 'news_radar',
          vector_size: 768,
          distance: 'Cosine',
          points_count: 54821,
        },
      ],
    };

    const embed: EmbedResponse = {
      vector: [0.12, 0.34],
      dimensions: 768,
    };

    expect(health.status).toBe('ok');
    expect(ingestRequest.documents).toHaveLength(1);
    expect(ingestResponse.ingested).toBe(8);
    expect(collections.collections[0].name).toBe('news_radar');
    expect(embed.dimensions).toBe(768);
  });
});
