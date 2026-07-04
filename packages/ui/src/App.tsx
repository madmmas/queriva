import { useMemo } from 'react';
import { ErrorBanner } from './components/ErrorBanner';
import { FilterStrip } from './components/FilterStrip';
import { RagPanel } from './components/RagPanel';
import { ResultsList } from './components/ResultsList';
import { SearchBar } from './components/SearchBar';
import { StatsPanel } from './components/StatsPanel';
import { TopBar } from './components/TopBar';
import { useSearch } from './hooks/useSearch';
import { useTheme } from './hooks/useTheme';
import type { SearchResult } from './types/api';
import { computeSearchStats } from './utils/searchStats';
import './App.css';

const DEFAULT_COLLECTION = import.meta.env.VITE_DEFAULT_COLLECTION ?? 'news_radar';
const API_BASE_URL = import.meta.env.VITE_API_URL ?? '';

/**
 * Root application component for the Queriva standalone SPA (issue #23).
 */
function App() {
  const { theme, toggleTheme } = useTheme();
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
    apiBaseUrl: API_BASE_URL,
    collection: DEFAULT_COLLECTION,
  });

  const stats = useMemo(
    () => computeSearchStats(results, latencyMs?.total ?? null),
    [results, latencyMs],
  );

  const handleResultClick = (result: SearchResult) => {
    window.open(result.url, '_blank', 'noopener,noreferrer');
  };

  const handleSuggestionClick = (suggestedQuery: string) => {
    search(suggestedQuery);
  };

  return (
    <div className="qv-app">
      <TopBar apiBaseUrl={API_BASE_URL} theme={theme} onThemeToggle={toggleTheme} />
      <section className="qv-search-zone" aria-label="Search">
        <SearchBar
          query={query}
          mode={mode}
          onQueryChange={setQuery}
          onModeChange={setMode}
          onSubmit={search}
        />
        <FilterStrip
          collection={DEFAULT_COLLECTION}
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

export default App;
