import {
  DEFAULT_MIN_SCORE,
  JSON_CONTENT_TYPE,
  SEARCH_API_PATH,
} from '../constants/search';
import type { SearchFilters, SearchMode, SearchResponse } from '../types/api';

/** Parameters for POST /api/search (SPEC §6). */
export interface SearchApiRequest {
  query: string;
  collection: string;
  mode: SearchMode;
  topK: number;
  minScore?: number;
  filters?: SearchFilters | null;
}

/**
 * Calls POST /api/search and returns the typed response body.
 */
export async function postSearch(
  apiBaseUrl: string,
  request: SearchApiRequest,
): Promise<SearchResponse> {
  const response = await fetch(`${apiBaseUrl}${SEARCH_API_PATH}`, {
    method: 'POST',
    headers: {
      'Content-Type': JSON_CONTENT_TYPE,
    },
    body: JSON.stringify({
      query: request.query,
      collection: request.collection,
      top_k: request.topK,
      min_score: request.minScore ?? DEFAULT_MIN_SCORE,
      mode: request.mode,
      filters: request.filters ?? null,
    }),
  });

  if (!response.ok) {
    let message = `Search failed with status ${response.status}. Check that the API is running at ${apiBaseUrl || 'the configured VITE_API_URL'}.`;
    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      const errorBody = (await response.json()) as { error?: string; message?: string };
      message = errorBody.error ?? errorBody.message ?? message;
    }
    throw new Error(message);
  }

  return (await response.json()) as SearchResponse;
}
