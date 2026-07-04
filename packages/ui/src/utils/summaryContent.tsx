import type { ReactNode } from 'react';

const SUMMARY_REFERENCE_PATTERN = /\[(\d+)\]/g;

/**
 * Splits RAG summary text into plain spans and inline reference badges for [N] citations.
 */
export function renderSummaryWithReferences(summary: string): ReactNode[] {
  const segments: ReactNode[] = [];
  let lastIndex = 0;
  const pattern = new RegExp(SUMMARY_REFERENCE_PATTERN);

  for (const match of summary.matchAll(pattern)) {
    const matchIndex = match.index ?? 0;

    if (matchIndex > lastIndex) {
      segments.push(summary.slice(lastIndex, matchIndex));
    }

    segments.push(
      <span key={`ref-${matchIndex}`} className="qv-ref">
        {match[1]}
      </span>,
    );
    lastIndex = matchIndex + match[0].length;
  }

  if (lastIndex < summary.length) {
    segments.push(summary.slice(lastIndex));
  }

  return segments;
}
