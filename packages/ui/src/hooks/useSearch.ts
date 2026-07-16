import { useCallback, useEffect, useReducer, useRef } from 'react';
import { DEFAULT_MIN_SCORE, DEFAULT_TOP_K, TOP_K_INCREMENT } from '../constants/search';
import type { SearchLatencyMs, SearchMode, SearchResponse, SearchResult } from '../types/api';
import {
  EMPTY_ACTIVE_FILTERS,
  toSearchFilters,
  type ActiveFilters,
} from '../utils/filterState';
import { postSearch } from '../utils/searchApi';

/** useSearch lifecycle states (issue #23). */
export type SearchStatus = 'idle' | 'loading' | 'success' | 'error';

interface SearchState {
  status: SearchStatus;
  query: string;
  mode: SearchMode;
  filters: ActiveFilters;
  topK: number;
  response: SearchResponse | null;
  error: string | null;
}

type SearchAction =
  | { type: 'SET_QUERY'; query: string }
  | { type: 'SET_MODE'; mode: SearchMode }
  | { type: 'SET_FILTERS'; filters: ActiveFilters }
  | { type: 'SET_TOP_K'; topK: number }
  | { type: 'SEARCH_REQUEST' }
  | { type: 'SEARCH_SUCCESS'; response: SearchResponse }
  | { type: 'SEARCH_FAILURE'; error: string }
  | { type: 'RESET' };

interface UseSearchOptions {
  apiBaseUrl: string;
  collection: string;
  defaultTopK?: number;
  defaultMinScore?: number;
  defaultMode?: SearchMode;
  initialFilters?: ActiveFilters;
}

export interface UseSearchResult {
  query: string;
  mode: SearchMode;
  filters: ActiveFilters;
  status: SearchStatus;
  loading: boolean;
  results: SearchResult[];
  summary: string | null;
  latencyMs: SearchLatencyMs | null;
  error: string | null;
  topK: number;
  remainingCount: number;
  setQuery: (query: string) => void;
  setMode: (mode: SearchMode) => void;
  setFilters: (filters: ActiveFilters) => void;
  search: (overrideQuery?: string) => void;
  loadMore: () => void;
  refresh: () => void;
}

const initialState: SearchState = {
  status: 'idle',
  query: '',
  mode: 'search',
  filters: EMPTY_ACTIVE_FILTERS,
  topK: DEFAULT_TOP_K,
  response: null,
  error: null,
};

function searchReducer(state: SearchState, action: SearchAction): SearchState {
  switch (action.type) {
    case 'SET_QUERY':
      return { ...state, query: action.query };
    case 'SET_MODE':
      return { ...state, mode: action.mode };
    case 'SET_FILTERS':
      return { ...state, filters: action.filters };
    case 'SET_TOP_K':
      return { ...state, topK: action.topK };
    case 'SEARCH_REQUEST':
      return { ...state, status: 'loading', error: null };
    case 'SEARCH_SUCCESS':
      return {
        ...state,
        status: 'success',
        response: action.response,
        error: null,
        query: action.response.query,
        mode: action.response.mode,
      };
    case 'SEARCH_FAILURE':
      return { ...state, status: 'error', response: null, error: action.error };
    case 'RESET':
      return {
        ...initialState,
        query: state.query,
        mode: state.mode,
        filters: state.filters,
        topK: state.topK,
      };
    default:
      return state;
  }
}

/**
 * Search state and POST /api/search integration with useReducer (issue #23).
 */
export function useSearch({
  apiBaseUrl,
  collection,
  defaultTopK = DEFAULT_TOP_K,
  defaultMinScore = DEFAULT_MIN_SCORE,
  defaultMode = 'search',
  initialFilters = EMPTY_ACTIVE_FILTERS,
}: UseSearchOptions): UseSearchResult {
  const [state, dispatch] = useReducer(searchReducer, {
    ...initialState,
    topK: defaultTopK,
    mode: defaultMode,
    filters: initialFilters,
  });
  const hasSearchedRef = useRef(false);

  const runSearch = useCallback(
    async (searchState: SearchState) => {
      const trimmedQuery = searchState.query.trim();
      if (!trimmedQuery) {
        dispatch({ type: 'RESET' });
        hasSearchedRef.current = false;
        return;
      }

      dispatch({ type: 'SEARCH_REQUEST' });

      try {
        const response = await postSearch(apiBaseUrl, {
          query: trimmedQuery,
          collection,
          mode: searchState.mode,
          topK: searchState.topK,
          minScore: defaultMinScore,
          filters: toSearchFilters(searchState.filters),
        });

        dispatch({ type: 'SEARCH_SUCCESS', response });
      } catch (searchError) {
        const message =
          searchError instanceof Error
            ? searchError.message
            : 'Search request failed. Verify the API is reachable.';
        dispatch({ type: 'SEARCH_FAILURE', error: message });
      }
    },
    [apiBaseUrl, collection, defaultMinScore],
  );

  const search = useCallback(
    (overrideQuery?: string) => {
      const queryToUse = overrideQuery ?? state.query;
      if (overrideQuery !== undefined) {
        dispatch({ type: 'SET_QUERY', query: overrideQuery });
      }

      hasSearchedRef.current = true;
      void runSearch({ ...state, query: queryToUse });
    },
    [runSearch, state],
  );

  const refresh = useCallback(() => {
    if (!state.query.trim()) {
      return;
    }
    hasSearchedRef.current = true;
    void runSearch(state);
  }, [runSearch, state]);

  const loadMore = useCallback(() => {
    const nextTopK = state.topK + TOP_K_INCREMENT;
    dispatch({ type: 'SET_TOP_K', topK: nextTopK });
    hasSearchedRef.current = true;
    void runSearch({ ...state, topK: nextTopK });
  }, [runSearch, state]);

  const setQuery = useCallback((query: string) => {
    dispatch({ type: 'SET_QUERY', query });
  }, []);

  const setMode = useCallback((mode: SearchMode) => {
    dispatch({ type: 'SET_MODE', mode });
  }, []);

  const setFilters = useCallback((filters: ActiveFilters) => {
    dispatch({ type: 'SET_FILTERS', filters });
  }, []);

  useEffect(() => {
    if (!hasSearchedRef.current || !state.query.trim()) {
      return;
    }

    void runSearch(state);
  }, [state.mode, state.filters]);

  const results = state.response?.results ?? [];
  const remainingCount =
    state.status === 'success' && results.length >= state.topK ? TOP_K_INCREMENT : 0;

  return {
    query: state.query,
    mode: state.mode,
    filters: state.filters,
    status: state.status,
    loading: state.status === 'loading',
    results,
    summary: state.response?.summary ?? null,
    latencyMs: state.response?.latency_ms ?? null,
    error: state.error,
    topK: state.topK,
    remainingCount,
    setQuery,
    setMode,
    setFilters,
    search,
    loadMore,
    refresh,
  };
}
