import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('/api/health', () =>
    HttpResponse.json({
      status: 'ok',
      qdrant: 'connected',
      ollama: 'connected',
      embed_sidecar: 'connected',
    }),
  ),
];
