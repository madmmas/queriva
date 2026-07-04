import { IconClock } from '@tabler/icons-react';
import {
  LATENCY_EMBED_LABEL,
  LATENCY_LLM_LABEL,
  LATENCY_SEARCH_LABEL,
} from '../constants/ui';
import type { SearchLatencyMs } from '../types/api';
import { formatStageLatency } from '../utils/formatters';

interface LatencyFooterProps {
  latencyMs: SearchLatencyMs;
}

/**
 * Embed, search, and LLM latency breakdown for the AI summary card (issue #22).
 */
export function LatencyFooter({ latencyMs }: LatencyFooterProps) {
  const synthesisMs = latencyMs.synthesis ?? 0;

  return (
    <span className="qv-latency">
      <IconClock size={12} aria-hidden="true" />
      {LATENCY_EMBED_LABEL} <b>{formatStageLatency(latencyMs.embed)}</b>
      {' · '}
      {LATENCY_SEARCH_LABEL} <b>{formatStageLatency(latencyMs.search)}</b>
      {' · '}
      {LATENCY_LLM_LABEL} <b>{formatStageLatency(synthesisMs)}</b>
    </span>
  );
}
