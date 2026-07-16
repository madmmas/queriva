import type { SearchMode, SearchResult } from './api';

/**
 * Props for the embeddable Module Federation SearchWidget (SPEC §11).
 */
export interface SearchWidgetProps {
  /** Queriva API base URL (required). */
  apiUrl: string;
  /** Qdrant collection name (required). */
  collection: string;
  /** Search bar placeholder text. */
  placeholder?: string;
  /** Initial search mode. Defaults to `search`. */
  defaultMode?: SearchMode;
  /** Colour theme. Defaults to `auto` (system preference). */
  theme?: 'light' | 'dark' | 'auto';
  /** Optional initial filters applied to search requests. */
  filters?: {
    language?: string;
    category?: string;
  };
  /** Called when a result card is activated. */
  onResultClick?: (result: SearchResult) => void;
}
