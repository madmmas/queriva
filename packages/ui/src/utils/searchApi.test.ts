import { describe, expect, it, vi } from 'vitest';
import { postSearch } from './searchApi';

describe('postSearch', () => {
  it('should throw actionable error when response is not ok', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      headers: {
        get: () => 'application/json',
      },
      json: async () => ({ error: 'Embed sidecar unavailable' }),
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(
      postSearch('', {
        query: 'floods in Dhaka',
        collection: 'news_radar',
        mode: 'search',
        topK: 10,
      }),
    ).rejects.toThrow('Embed sidecar unavailable');

    vi.unstubAllGlobals();
  });
});
