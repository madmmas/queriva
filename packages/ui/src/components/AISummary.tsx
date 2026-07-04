import { IconCopy, IconCpu, IconRefresh } from '@tabler/icons-react';
import { DEFAULT_LLM_MODEL_LABEL } from '../constants/display';
import {
  AI_RAG_HEADER_LABEL,
  COPY_SUMMARY_LABEL,
  REFRESH_SUMMARY_LABEL,
} from '../constants/ui';
import type { SearchLatencyMs } from '../types/api';
import { renderSummaryWithReferences } from '../utils/summaryContent';
import { LatencyFooter } from './LatencyFooter';

interface AISummaryProps {
  summary: string;
  latencyMs: SearchLatencyMs;
  modelLabel?: string;
  onCopy?: () => void;
  onRefresh?: () => void;
}

/**
 * RAG summary card with reference badges, latency footer, and copy/refresh actions (issue #22).
 */
export function AISummary({
  summary,
  latencyMs,
  modelLabel = DEFAULT_LLM_MODEL_LABEL,
  onCopy,
  onRefresh,
}: AISummaryProps) {
  const handleCopy = async () => {
    await navigator.clipboard.writeText(summary);
    onCopy?.();
  };

  return (
    <div className="qv-ai-card">
      <div className="qv-ai-head">
        <div className="qv-ai-dot" aria-hidden="true" />
        <span className="qv-ai-label">{AI_RAG_HEADER_LABEL}</span>
        <div className="qv-ai-model">
          <IconCpu size={11} aria-hidden="true" />
          {modelLabel}
        </div>
      </div>
      <div className="qv-ai-body">{renderSummaryWithReferences(summary)}</div>
      <div className="qv-ai-footer">
        <LatencyFooter latencyMs={latencyMs} />
        <button
          type="button"
          className="qv-ai-action"
          aria-label={COPY_SUMMARY_LABEL}
          onClick={() => {
            void handleCopy();
          }}
        >
          <IconCopy size={13} aria-hidden="true" />
        </button>
        {onRefresh ? (
          <button
            type="button"
            className="qv-ai-action"
            aria-label={REFRESH_SUMMARY_LABEL}
            onClick={onRefresh}
          >
            <IconRefresh size={13} aria-hidden="true" />
          </button>
        ) : null}
      </div>
    </div>
  );
}
