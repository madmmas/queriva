/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
  readonly VITE_DEFAULT_COLLECTION?: string;
  /** SPEC §13: mirrors SEARCH_MIN_SCORE for the standalone UI. */
  readonly VITE_SEARCH_MIN_SCORE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module '*.css' {
  const content: string;
  export default content;
}
