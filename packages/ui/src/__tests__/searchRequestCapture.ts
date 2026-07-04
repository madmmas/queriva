import type { SearchRequest } from '../types/api';

/** Last POST /api/search bodies received by MSW handlers (issue #24). */
export const capturedSearchRequests: SearchRequest[] = [];

/** Clears captured search requests between tests. */
export function resetCapturedSearchRequests(): void {
  capturedSearchRequests.length = 0;
}

/** Records one search request for assertions in integration tests. */
export function recordSearchRequest(request: SearchRequest): void {
  capturedSearchRequests.push(request);
}
