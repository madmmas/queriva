import {
  LATENCY_SECONDS_DECIMAL_PLACES,
  MAX_SCORE_BAR_WIDTH_PERCENT,
  MIN_SCORE_BAR_WIDTH_PERCENT,
  MILLISECONDS_PER_SECOND,
  SCORE_DECIMAL_PLACES,
} from '../constants/display';
import { LOAD_MORE_SUFFIX } from '../constants/ui';

/**
 * Formats a cosine similarity score for display (e.g. 0.92).
 */
export function formatScore(score: number): string {
  return score.toFixed(SCORE_DECIMAL_PLACES);
}

/**
 * Returns score bar fill width as a percentage string for inline style.
 */
export function scoreBarWidthPercent(score: number): string {
  const clamped = Math.min(
    MAX_SCORE_BAR_WIDTH_PERCENT,
    Math.max(MIN_SCORE_BAR_WIDTH_PERCENT, score * MAX_SCORE_BAR_WIDTH_PERCENT),
  );
  return `${clamped}%`;
}

/**
 * Formats an ISO published_at timestamp for result cards (e.g. Jun 15, 2026).
 */
export function formatPublishedDate(publishedAt: string): string {
  const parsed = new Date(publishedAt);
  if (Number.isNaN(parsed.getTime())) {
    return publishedAt;
  }

  return parsed.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

/**
 * Formats total latency milliseconds as seconds for the stats panel.
 */
export function formatLatencySeconds(totalTimeMs: number): string {
  const seconds = totalTimeMs / MILLISECONDS_PER_SECOND;
  return seconds.toFixed(LATENCY_SECONDS_DECIMAL_PLACES);
}

/**
 * Formats the load-more button label with a remaining result count.
 */
export function formatLoadMoreLabel(remainingCount: number): string {
  return `${remainingCount} ${LOAD_MORE_SUFFIX}`;
}

/**
 * Returns an uppercase language badge label (BN, EN).
 */
export function formatLanguageBadge(language: string): string {
  return language.trim().toUpperCase();
}
