import { describe, expect, it } from 'vitest';
import { buildSearchResponse } from './handlers';

describe('MSW search handlers', () => {
  it('should return search mode response without summary', () => {
    const response = buildSearchResponse({
      query: 'floods in Dhaka last week',
      collection: 'news_radar',
      mode: 'search',
    });

    expect(response.mode).toBe('search');
    expect(response.summary).toBeNull();
    expect(response.results.length).toBeGreaterThan(0);
  });

  it('should return rag mode response with summary', () => {
    const response = buildSearchResponse({
      query: 'floods in Dhaka last week',
      collection: 'news_radar',
      mode: 'rag',
    });

    expect(response.mode).toBe('rag');
    expect(response.summary).toContain('Three flood events hit Dhaka');
    expect(response.latency_ms.synthesis).toBeGreaterThan(0);
  });
});
