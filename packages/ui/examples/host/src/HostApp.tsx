import { Suspense, lazy, useCallback, useState } from 'react';
import type { SearchResult } from '../../../src/types/api';

const SearchWidget = lazy(() => import('queriva/SearchWidget'));

const DEFAULT_API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
const DEFAULT_COLLECTION = import.meta.env.VITE_DEFAULT_COLLECTION ?? 'news_radar';

/**
 * Minimal host shell demonstrating Module Federation consumption (SPEC §11).
 */
export function HostApp() {
  const [lastClickedId, setLastClickedId] = useState<string | null>(null);

  const handleResultClick = useCallback((result: SearchResult) => {
    setLastClickedId(result.id);
  }, []);

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', padding: '1.5rem' }}>
      <header style={{ marginBottom: '1rem' }}>
        <h1 style={{ margin: 0 }}>Queriva host example</h1>
        <p style={{ margin: '0.5rem 0 0', color: '#555' }}>
          Loads <code>SearchWidget</code> from{' '}
          <code>{import.meta.env.VITE_QUERIVA_REMOTE_ENTRY ?? 'http://localhost:5173/assets/remoteEntry.js'}</code>
        </p>
        {lastClickedId ? (
          <p role="status">Last clicked result id: {lastClickedId}</p>
        ) : null}
      </header>
      <Suspense fallback={<p>Loading SearchWidget…</p>}>
        <SearchWidget
          apiUrl={DEFAULT_API_URL}
          collection={DEFAULT_COLLECTION}
          defaultMode="search"
          theme="light"
          onResultClick={handleResultClick}
        />
      </Suspense>
    </div>
  );
}
