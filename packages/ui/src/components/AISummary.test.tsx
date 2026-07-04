import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { demoRagSearchResponse } from '../fixtures/demoRagSearchResponse';
import { COPY_SUMMARY_LABEL, REFRESH_SUMMARY_LABEL } from '../constants/ui';
import { AISummary } from './AISummary';

describe('AISummary', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should write summary text to clipboard when copy button is clicked', async () => {
    const user = userEvent.setup();
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.spyOn(navigator.clipboard, 'writeText').mockImplementation(writeText);
    const summary = demoRagSearchResponse.summary ?? '';

    render(
      <AISummary summary={summary} latencyMs={demoRagSearchResponse.latency_ms} />,
    );

    await user.click(screen.getByRole('button', { name: COPY_SUMMARY_LABEL }));

    expect(writeText).toHaveBeenCalledWith(summary);
  });

  it('should call onRefresh when refresh button is clicked', async () => {
    const user = userEvent.setup();
    const onRefresh = vi.fn();
    const summary = demoRagSearchResponse.summary ?? '';

    render(
      <AISummary
        summary={summary}
        latencyMs={demoRagSearchResponse.latency_ms}
        onRefresh={onRefresh}
      />,
    );

    await user.click(screen.getByRole('button', { name: REFRESH_SUMMARY_LABEL }));

    expect(onRefresh).toHaveBeenCalledTimes(1);
  });

  it('should render reference badges for numeric citations in summary text', () => {
    const { container } = render(
      <AISummary
        summary="Floods hit Dhaka. [1] Relief efforts expanded. [2]"
        latencyMs={demoRagSearchResponse.latency_ms}
      />,
    );

    const badges = container.querySelectorAll('.qv-ref');
    expect(badges).toHaveLength(2);
    expect(badges[0]).toHaveTextContent('1');
    expect(badges[1]).toHaveTextContent('2');
  });
});
