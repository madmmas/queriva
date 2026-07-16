/** POST /api/search path (SPEC §6). */
export const SEARCH_API_PATH = '/api/search';

/** Default result count per SPEC §6 / SEARCH_DEFAULT_TOP_K. */
export const DEFAULT_TOP_K = 10;

/**
 * Default min_score per SPEC §6 / SEARCH_MIN_SCORE / VITE_SEARCH_MIN_SCORE.
 * 0.40 fits LaBSE cosine scores on the news_radar fixture; raise for stricter filtering.
 */
export const DEFAULT_MIN_SCORE = 0.40;

/** top_k increment when loading more results. */
export const TOP_K_INCREMENT = 10;

/** Vite dev server port per SPEC §12 ui service (standalone SPA). */
export const DEV_SERVER_PORT = 3000;

/**
 * Federation remote preview port (SPEC §11 / ADR-010).
 * Host loads remoteEntry from http://localhost:5173/assets/remoteEntry.js
 */
export const REMOTE_PREVIEW_PORT = 5173;

/** Default remoteEntry URL for example host apps (SPEC §11). */
export const DEFAULT_REMOTE_ENTRY_URL = 'http://localhost:5173/assets/remoteEntry.js';

/** JSON content type for search POST requests. */
export const JSON_CONTENT_TYPE = 'application/json';

/**
 * Resolves min_score from a VITE_SEARCH_MIN_SCORE string, falling back to {@link DEFAULT_MIN_SCORE}.
 */
export function resolveMinScoreFromEnv(envValue?: string): number {
  if (envValue === undefined || envValue.trim() === '') {
    return DEFAULT_MIN_SCORE;
  }
  const parsed = Number.parseFloat(envValue);
  if (!Number.isFinite(parsed) || parsed < 0 || parsed > 1) {
    return DEFAULT_MIN_SCORE;
  }
  return parsed;
}
