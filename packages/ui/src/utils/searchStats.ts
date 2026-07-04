import type { SearchResult } from '../types/api';

/** Aggregated metrics shown in the stats panel. */
export interface SearchStats {
  hitCount: number;
  bestScore: number | null;
  languageCount: number;
  totalTimeMs: number | null;
}

/**
 * Derives stats panel values from search results and optional latency.
 */
export function computeSearchStats(
  results: SearchResult[],
  totalTimeMs: number | null,
): SearchStats {
  if (results.length === 0) {
    return {
      hitCount: 0,
      bestScore: null,
      languageCount: 0,
      totalTimeMs,
    };
  }

  const languages = new Set(results.map((result) => result.language));
  const bestScore = results.reduce(
    (currentBest, result) => Math.max(currentBest, result.score),
    results[0].score,
  );

  return {
    hitCount: results.length,
    bestScore,
    languageCount: languages.size,
    totalTimeMs,
  };
}
