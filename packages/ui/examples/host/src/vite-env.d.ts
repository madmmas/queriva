/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_QUERIVA_REMOTE_ENTRY?: string;
  readonly VITE_API_URL?: string;
  readonly VITE_DEFAULT_COLLECTION?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module 'queriva/SearchWidget' {
  import type { ComponentType } from 'react';
  import type { SearchMode, SearchResult } from '../../../src/types/api';

  export interface SearchWidgetProps {
    apiUrl: string;
    collection: string;
    placeholder?: string;
    defaultMode?: SearchMode;
    theme?: 'light' | 'dark' | 'auto';
    filters?: {
      language?: string;
      category?: string;
    };
    onResultClick?: (result: SearchResult) => void;
  }

  const SearchWidget: ComponentType<SearchWidgetProps>;
  export default SearchWidget;
  export type { SearchWidgetProps as FederatedSearchWidgetProps };
}
