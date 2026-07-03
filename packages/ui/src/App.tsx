import { useState } from 'react';
import { FilterStrip } from './components/FilterStrip';
import { SearchBar } from './components/SearchBar';
import { TopBar } from './components/TopBar';
import { SCAFFOLD_STATUS_MESSAGE } from './constants/ui';
import { useTheme } from './hooks/useTheme';
import type { SearchMode } from './types/api';
import { EMPTY_ACTIVE_FILTERS } from './utils/filterState';
import './App.css';

const DEFAULT_COLLECTION = import.meta.env.VITE_DEFAULT_COLLECTION ?? 'news_radar';
const API_BASE_URL = import.meta.env.VITE_API_URL ?? '';

/**
 * Root application component for the Queriva standalone SPA.
 */
function App() {
  const { theme, toggleTheme } = useTheme();
  const [query, setQuery] = useState('');
  const [mode, setMode] = useState<SearchMode>('search');
  const [filters, setFilters] = useState(EMPTY_ACTIVE_FILTERS);
  const [resultCount] = useState(0);

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
          resultCount={resultCount}
          onFiltersChange={setFilters}
        />
      </section>
      <main className="qv-main">
        <p className="qv-placeholder-message">{SCAFFOLD_STATUS_MESSAGE}</p>
      </main>
    </div>
  );
}

export default App;
