import { http, HttpResponse } from 'msw';
import type { HealthResponse } from '../types/api';

const mockHealthResponse: HealthResponse = {
  status: 'ok',
  qdrant: 'connected',
  ollama: 'connected',
  embed_sidecar: 'connected',
};

export const handlers = [
  http.get('/api/health', () => HttpResponse.json(mockHealthResponse)),
];
