import { renderHook, waitFor, act } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { server } from '../setupTests';
import { demoSearchResponse } from '../fixtures/demoSearchResponse';
import { useSearch } from './useSearch';

describe('useSearch', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should return success results after search is triggered', async () => {
    const { result } = renderHook(() =>
      useSearch({ apiBaseUrl: '', collection: 'news_radar' }),
    );

    result.current.search('floods in Dhaka last week');

    await waitFor(() => expect(result.current.status).toBe('success'));

    expect(result.current.results).toHaveLength(demoSearchResponse.results.length);
    expect(result.current.summary).toBeNull();
    expect(result.current.error).toBeNull();
  });

  it('should set error state when search returns 500', async () => {
    server.use(
      http.post('/api/search', () => HttpResponse.json({ error: 'Internal error' }, { status: 500 })),
    );

    const { result } = renderHook(() =>
      useSearch({ apiBaseUrl: '', collection: 'news_radar' }),
    );

    result.current.search('floods in Dhaka last week');

    await waitFor(() => expect(result.current.status).toBe('error'));

    expect(result.current.error).toContain('Internal error');
    expect(result.current.results).toHaveLength(0);
  });

  it('should return summary when mode is rag', async () => {
    const { result } = renderHook(() =>
      useSearch({ apiBaseUrl: '', collection: 'news_radar' }),
    );

    await act(async () => {
      result.current.setMode('rag');
    });

    result.current.search('floods in Dhaka last week');

    await waitFor(() => expect(result.current.status).toBe('success'));

    expect(result.current.summary).toContain('Three flood events hit Dhaka');
  });
});
