import { IconSparkles } from '@tabler/icons-react';
import { AI_SUMMARY_LABEL } from '../constants/ui';
import type { SearchLatencyMs, SearchMode } from '../types/api';
import { AISummary } from './AISummary';
import { SuggestionsPanel } from './SuggestionsPanel';

interface RagPanelProps {
  mode: SearchMode;
  summary: string | null | undefined;
  latencyMs: SearchLatencyMs;
  onRefresh?: () => void;
  onSuggestionClick: (query: string) => void;
}

/**
 * Right-column RAG summary and suggestions; hidden in search mode or when summary is absent (issue #22).
 */
export function RagPanel({
  mode,
  summary,
  latencyMs,
  onRefresh,
  onSuggestionClick,
}: RagPanelProps) {
  if (mode !== 'rag' || !summary) {
    return null;
  }

  return (
    <>
      <div className="qv-sec-label">
        <IconSparkles size={13} aria-hidden="true" />
        {AI_SUMMARY_LABEL}
      </div>
      <AISummary summary={summary} latencyMs={latencyMs} onRefresh={onRefresh} />
      <SuggestionsPanel onSuggestionClick={onSuggestionClick} />
    </>
  );
}
