import { http, HttpResponse } from 'msw';
import { demoRagSearchResponse } from '../fixtures/demoRagSearchResponse';
import { demoSearchResponse } from '../fixtures/demoSearchResponse';
import type { HealthResponse, SearchRequest, SearchResponse } from '../types/api';

const mockHealthResponse: HealthResponse = {
  status: 'ok',
  qdrant: 'connected',
  ollama: 'connected',
  embed_sidecar: 'connected',
};

function buildSearchResponse(request: SearchRequest): SearchResponse {
  if (request.mode === 'rag') {
    return {
      ...demoRagSearchResponse,
      query: request.query,
      mode: 'rag',
    };
  }

  return {
    ...demoSearchResponse,
    query: request.query,
    mode: 'search',
    summary: null,
  };
}

export const handlers = [
  http.get('/api/health', () => HttpResponse.json(mockHealthResponse)),
  http.post('/api/search', async ({ request }) => {
    const body = (await request.json()) as SearchRequest;
    return HttpResponse.json(buildSearchResponse(body));
  }),
];
