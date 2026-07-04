/** POST /api/search path (SPEC §6). */
export const SEARCH_API_PATH = '/api/search';

/** Default result count per SPEC §6 / SEARCH_DEFAULT_TOP_K. */
export const DEFAULT_TOP_K = 10;

/** Default min_score per SPEC §6 / SEARCH_MIN_SCORE. */
export const DEFAULT_MIN_SCORE = 0.60;

/** top_k increment when loading more results. */
export const TOP_K_INCREMENT = 10;

/** Vite dev server port per SPEC §12 ui service. */
export const DEV_SERVER_PORT = 3000;

/** JSON content type for search POST requests. */
export const JSON_CONTENT_TYPE = 'application/json';
