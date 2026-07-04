import { useMemo, useState } from 'react';
import { demoRagSearchResponse } from './fixtures/demoRagSearchResponse';
import { demoSearchResponse } from './fixtures/demoSearchResponse';
import { FilterStrip } from './components/FilterStrip';
import { RagPanel } from './components/RagPanel';
import { ResultsList } from './components/ResultsList';
import { SearchBar } from './components/SearchBar';
import { StatsPanel } from './components/StatsPanel';
import { TopBar } from './components/TopBar';
import { useTheme } from './hooks/useTheme';
import type { SearchMode, SearchResult } from './types/api';
import { EMPTY_ACTIVE_FILTERS } from './utils/filterState';
import { computeSearchStats } from './utils/searchStats';
import './App.css';

const DEFAULT_COLLECTION = import.meta.env.VITE_DEFAULT_COLLECTION ?? 'news_radar';
const API_BASE_URL = import.meta.env.VITE_API_URL ?? '';
const DEFAULT_TOP_K = 10;
const DEMO_LOADING = false;
const DEMO_REMAINING_COUNT = 4;

/**
 * Root application component for the Queriva standalone SPA.
 */
function App() {
  const { theme, toggleTheme } = useTheme();
  const [query, setQuery] = useState(demoSearchResponse.query);
  const [mode, setMode] = useState<SearchMode>('search');
  const [filters, setFilters] = useState(EMPTY_ACTIVE_FILTERS);
  const [topK, setTopK] = useState(DEFAULT_TOP_K);
  const [results] = useState<SearchResult[]>(demoSearchResponse.results);

  const visibleResults = useMemo(() => results.slice(0, topK), [results, topK]);
  const activeSummary = mode === 'rag' ? demoRagSearchResponse.summary : null;
  const activeLatency =
    mode === 'rag' ? demoRagSearchResponse.latency_ms : demoSearchResponse.latency_ms;
  const stats = useMemo(
    () => computeSearchStats(visibleResults, activeLatency.total),
    [visibleResults, activeLatency.total],
  );

  return (
    <div className="qv-app">
      <TopBar apiBaseUrl={API_BASE_URL} theme={theme} onThemeToggle={toggleTheme} />
      <section className="qv-search-zone" aria-label="Search">
        <SearchBar
          query={query}
          mode={mode}
          onQueryChange={setQuery}
          onModeChange={setMode}
          onSubmit={() => undefined}
        />
        <FilterStrip
          collection={DEFAULT_COLLECTION}
          filters={filters}
          resultCount={visibleResults.length}
          onFiltersChange={setFilters}
        />
      </section>
      <main className="qv-body">
        <ResultsList
          query={query}
          results={visibleResults}
          loading={DEMO_LOADING}
          remainingCount={DEMO_REMAINING_COUNT}
          onLoadMore={() => setTopK((current) => current + DEFAULT_TOP_K)}
        />
        <aside className="qv-right" aria-label="Search statistics and AI summary">
          <StatsPanel stats={stats} />
          <RagPanel
            mode={mode}
            summary={activeSummary}
            latencyMs={activeLatency}
            onRefresh={() => setQuery((current) => current)}
            onSuggestionClick={setQuery}
          />
        </aside>
      </main>
    </div>
  );
}

export default App;
