import { describe, expect, it } from 'vitest';
import { EMPTY_ACTIVE_FILTERS, toSearchFilters } from './filterState';

describe('filterState', () => {
  it('should map active filters to spec search filters', () => {
    const searchFilters = toSearchFilters({
      ...EMPTY_ACTIVE_FILTERS,
      language: 'bn',
      category: 'national',
    });

    expect(searchFilters).toEqual({
      language: 'bn',
      category: 'national',
    });
  });

  it('should return null when no filters are active', () => {
    expect(toSearchFilters(EMPTY_ACTIVE_FILTERS)).toBeNull();
  });
});
