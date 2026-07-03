import type { SearchFilters } from '../types/api';

/** Active UI filter chip state mapped to API search filters. */
export interface ActiveFilters {
  language: string | null;
  dateFrom: string | null;
  dateTo: string | null;
  category: string | null;
}

/** Empty filter state — no chips selected. */
export const EMPTY_ACTIVE_FILTERS: ActiveFilters = {
  language: null,
  dateFrom: null,
  dateTo: null,
  category: null,
};

/**
 * Converts active chip state to a SPEC §6 search filters object.
 */
export function toSearchFilters(activeFilters: ActiveFilters): SearchFilters | null {
  const searchFilters: SearchFilters = {};

  if (activeFilters.language) {
    searchFilters.language = activeFilters.language;
  }
  if (activeFilters.dateFrom) {
    searchFilters.date_from = activeFilters.dateFrom;
  }
  if (activeFilters.dateTo) {
    searchFilters.date_to = activeFilters.dateTo;
  }
  if (activeFilters.category) {
    searchFilters.category = activeFilters.category;
  }

  if (Object.keys(searchFilters).length === 0) {
    return null;
  }

  return searchFilters;
}

/**
 * Returns an ISO date string (YYYY-MM-DD) for a date offset from today.
 */
export function formatIsoDateDaysAgo(daysAgo: number, referenceDate: Date = new Date()): string {
  const date = new Date(referenceDate);
  date.setDate(date.getDate() - daysAgo);
  return date.toISOString().slice(0, 10);
}
