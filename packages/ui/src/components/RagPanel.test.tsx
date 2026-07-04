import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { demoRagSearchResponse } from '../fixtures/demoRagSearchResponse';
import { AI_SUMMARY_LABEL, SUGGESTIONS_LABEL } from '../constants/ui';
import { RagPanel } from './RagPanel';

describe('RagPanel', () => {
  it('should render nothing when mode is search', () => {
    const { container } = render(
      <RagPanel
        mode="search"
        summary={demoRagSearchResponse.summary}
        latencyMs={demoRagSearchResponse.latency_ms}
        onSuggestionClick={vi.fn()}
      />,
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('should render nothing when summary is null in rag mode', () => {
    const { container } = render(
      <RagPanel
        mode="rag"
        summary={null}
        latencyMs={demoRagSearchResponse.latency_ms}
        onSuggestionClick={vi.fn()}
      />,
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('should show ai summary and suggestions when mode is rag and summary is present', () => {
    render(
      <RagPanel
        mode="rag"
        summary={demoRagSearchResponse.summary}
        latencyMs={demoRagSearchResponse.latency_ms}
        onSuggestionClick={vi.fn()}
      />,
    );

    expect(screen.getByText(AI_SUMMARY_LABEL)).toBeInTheDocument();
    expect(screen.getByText(SUGGESTIONS_LABEL)).toBeInTheDocument();
    expect(screen.getByText(/Three flood events hit Dhaka/)).toBeInTheDocument();
  });
});
