import { IconArrowRight, IconBuilding, IconCloudRain, IconHeart } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { DEFAULT_SUGGESTIONS, type SuggestionItem } from '../constants/suggestions';
import { SUGGESTIONS_LABEL } from '../constants/ui';

interface SuggestionsPanelProps {
  suggestions?: readonly SuggestionItem[];
  onSuggestionClick: (query: string) => void;
}

const SUGGESTION_ICONS: ReactNode[] = [
  <IconHeart size={14} aria-hidden="true" key="heart" />,
  <IconCloudRain size={14} aria-hidden="true" key="rain" />,
  <IconBuilding size={14} aria-hidden="true" key="building" />,
];

/**
 * Follow-up query buttons shown below the RAG summary card (issue #22).
 */
export function SuggestionsPanel({
  suggestions = DEFAULT_SUGGESTIONS,
  onSuggestionClick,
}: SuggestionsPanelProps) {
  return (
    <>
      <div className="qv-sec-label qv-suggest-label">
        <IconArrowRight size={13} aria-hidden="true" />
        {SUGGESTIONS_LABEL}
      </div>
      {suggestions.map((suggestion, index) => (
        <button
          key={suggestion.query}
          type="button"
          className="qv-suggest-btn"
          onClick={() => onSuggestionClick(suggestion.query)}
        >
          {SUGGESTION_ICONS[index]}
          {suggestion.label}
        </button>
      ))}
    </>
  );
}
