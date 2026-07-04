/** One follow-up query shown in the RAG suggestions panel (issue #22). */
export interface SuggestionItem {
  query: string;
  label: string;
}

/** Default follow-up queries matching mock UI (issue #22). */
export const DEFAULT_SUGGESTIONS: readonly SuggestionItem[] = [
  {
    query: 'Search Queriva for Dhaka flood relief efforts June 2026',
    label: 'Relief efforts after Dhaka floods ↗',
  },
  {
    query: 'Search Queriva for monsoon rainfall records Bangladesh 2026',
    label: 'Monsoon rainfall records Bangladesh ↗',
  },
  {
    query: 'Search Queriva for WASA infrastructure flood response Dhaka',
    label: 'WASA flood infrastructure response ↗',
  },
] as const;
