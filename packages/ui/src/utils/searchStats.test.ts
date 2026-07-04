import { describe, expect, it } from 'vitest';
import { mockSearchResponse } from '../__tests__/mockSearchResponse';
import { computeSearchStats } from './searchStats';

describe('searchStats', () => {
  it('should compute hit count languages and best score from results', () => {
    const stats = computeSearchStats(mockSearchResponse.results, 1908);

    expect(stats.hitCount).toBe(2);
    expect(stats.bestScore).toBe(0.92);
    expect(stats.languageCount).toBe(2);
    expect(stats.totalTimeMs).toBe(1908);
  });
});
