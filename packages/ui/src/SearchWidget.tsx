import { useEffect, useMemo, useState } from 'react';
import { ErrorBanner } from './components/ErrorBanner';
import { FilterStrip } from './components/FilterStrip';
import { RagPanel } from './components/RagPanel';
import { ResultsList } from './components/ResultsList';
import { SearchBar } from './components/SearchBar';
import { StatsPanel } from './components/StatsPanel';
import { SEARCH_PLACEHOLDER } from './constants/ui';
import { useSearch } from './hooks/useSearch';
import type { SearchResult } from './types/api';
import type { SearchWidgetProps } from './types/widget';
import {
  EMPTY_ACTIVE_FILTERS,
  type ActiveFilters,
} from './utils/filterState';
import { computeSearchStats } from './utils/searchStats';
import './index.css';
import './App.css';

export type { SearchWidgetProps } from './types/widget';

/**
 * Resolves widget theme prop to a concrete light/dark value.
 */
function resolveWidgetTheme(theme: SearchWidgetProps['theme']): 'light' | 'dark' {
  if (theme === 'light' || theme === 'dark') {
    return theme;
  }

  if (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  ) {
    return 'dark';
  }

  return 'light';
}

/**
 * Maps optional SearchWidget filter props to ActiveFilters chip state.
 */
function toInitialFilters(filters: SearchWidgetProps['filters']): ActiveFilters {
  if (!filters) {
    return EMPTY_ACTIVE_FILTERS;
  }

  return {
    ...EMPTY_ACTIVE_FILTERS,
    language: filters.language ?? null,
    category: filters.category ?? null,
  };
}

/**
 * Embeddable Queriva search widget exported via Module Federation (SPEC §11, issue #25).
 *
 * Functional with only `apiUrl` + `collection`; all other props are optional.
 */
export default function SearchWidget({
  apiUrl,
  collection,
  placeholder = SEARCH_PLACEHOLDER,
  defaultMode = 'search',
  theme = 'auto',
  filters: filterProps,
  onResultClick,
}: SearchWidgetProps) {
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>(() =>
    resolveWidgetTheme(theme),
  );

  useEffect(() => {
    setResolvedTheme(resolveWidgetTheme(theme));
  }, [theme]);

  const {
    query,
    mode,
    filters,
    loading,
    results,
    summary,
    latencyMs,
    error,
    remainingCount,
    setQuery,
    setMode,
    setFilters,
    search,
    loadMore,
    refresh,
  } = useSearch({
    apiBaseUrl: apiUrl,
    collection,
    defaultMode,
    initialFilters: toInitialFilters(filterProps),
  });

  const stats = useMemo(
    () => computeSearchStats(results, latencyMs?.total ?? null),
    [results, latencyMs],
  );

  const handleResultClick = (result: SearchResult) => {
    if (onResultClick) {
      onResultClick(result);
      return;
    }
    window.open(result.url, '_blank', 'noopener,noreferrer');
  };

  const handleSuggestionClick = (suggestedQuery: string) => {
    search(suggestedQuery);
  };

  return (
    <div className="qv-app qv-widget" data-theme={resolvedTheme} data-testid="search-widget">
      <section className="qv-search-zone" aria-label="Search">
        <SearchBar
          query={query}
          mode={mode}
          placeholder={placeholder}
          onQueryChange={setQuery}
          onModeChange={setMode}
          onSubmit={search}
        />
        <FilterStrip
          collection={collection}
          filters={filters}
          resultCount={results.length}
          onFiltersChange={setFilters}
        />
        {error ? <ErrorBanner message={error} /> : null}
      </section>
      <main className="qv-body">
        <ResultsList
          query={query}
          results={results}
          loading={loading}
          remainingCount={remainingCount}
          onLoadMore={loadMore}
          onResultClick={handleResultClick}
        />
        <aside className="qv-right" aria-label="Search statistics and AI summary">
          <StatsPanel stats={stats} />
          {latencyMs ? (
            <RagPanel
              mode={mode}
              summary={summary}
              latencyMs={latencyMs}
              onRefresh={refresh}
              onSuggestionClick={handleSuggestionClick}
            />
          ) : null}
        </aside>
      </main>
    </div>
  );
}
