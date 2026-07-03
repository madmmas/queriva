import { IconListSearch, IconSparkles } from '@tabler/icons-react';
import {
  MODE_TOGGLE_GROUP_LABEL,
  RAG_MODE_LABEL,
  SEARCH_MODE_LABEL,
} from '../constants/ui';
import type { SearchMode } from '../types/api';

interface ModeToggleProps {
  mode: SearchMode;
  onModeChange: (mode: SearchMode) => void;
}

/**
 * Segmented Search / RAG mode control (issue #20).
 */
export function ModeToggle({ mode, onModeChange }: ModeToggleProps) {
  return (
    <div className="qv-mode-seg" role="radiogroup" aria-label={MODE_TOGGLE_GROUP_LABEL}>
      <button
        type="button"
        className={`qv-seg-btn${mode === 'search' ? ' on' : ''}`}
        role="radio"
        aria-checked={mode === 'search'}
        aria-label={SEARCH_MODE_LABEL}
        onClick={() => onModeChange('search')}
      >
        <IconListSearch size={14} aria-hidden="true" />
        {SEARCH_MODE_LABEL}
      </button>
      <button
        type="button"
        className={`qv-seg-btn${mode === 'rag' ? ' on' : ''}`}
        role="radio"
        aria-checked={mode === 'rag'}
        aria-label={RAG_MODE_LABEL}
        onClick={() => onModeChange('rag')}
      >
        <IconSparkles size={14} aria-hidden="true" />
        {RAG_MODE_LABEL}
      </button>
    </div>
  );
}
