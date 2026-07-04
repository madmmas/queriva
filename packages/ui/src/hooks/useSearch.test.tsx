import { renderHook, waitFor, act } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { capturedSearchRequests } from '../__tests__/searchRequestCapture';
import { server } from '../setupTests';
import { demoSearchResponse } from '../fixtures/demoSearchResponse';
import { FILTER_LANGUAGE_BANGLA } from '../constants/filters';
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

  it('should send filter parameters when filters change after search', async () => {
    const { result } = renderHook(() =>
      useSearch({ apiBaseUrl: '', collection: 'news_radar' }),
    );

    result.current.search('floods in Dhaka last week');
    await waitFor(() => expect(result.current.status).toBe('success'));

    await act(async () => {
      result.current.setFilters({
        language: FILTER_LANGUAGE_BANGLA,
        dateFrom: null,
        dateTo: null,
        category: null,
      });
    });

    await waitFor(() => {
      const lastRequest = capturedSearchRequests[capturedSearchRequests.length - 1];
      expect(lastRequest.filters?.language).toBe(FILTER_LANGUAGE_BANGLA);
    });
  });

  it('should increase top_k when loadMore is called', async () => {
    const { result } = renderHook(() =>
      useSearch({ apiBaseUrl: '', collection: 'news_radar' }),
    );

    result.current.search('floods in Dhaka last week');
    await waitFor(() => expect(result.current.status).toBe('success'));

    const initialTopK = result.current.topK;
    result.current.loadMore();

    await waitFor(() => expect(result.current.topK).toBeGreaterThan(initialTopK));
  });
});
