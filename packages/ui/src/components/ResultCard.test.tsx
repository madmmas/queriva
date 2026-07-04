import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { mockSearchResponse } from '../__tests__/mockSearchResponse';
import { NO_RESULTS_MESSAGE, SEARCH_SKELETON_LABEL } from '../constants/ui';
import { ResultCard } from './ResultCard';

describe('ResultCard', () => {
  it('should render all fields from mock api response', () => {
    const result = mockSearchResponse.results[0];

    render(<ResultCard rank={1} result={result} isTopResult />);

    expect(screen.getByText(result.title)).toBeInTheDocument();
    expect(screen.getByText(result.snippet)).toBeInTheDocument();
    expect(screen.getByText(result.source)).toBeInTheDocument();
    expect(screen.getByText('BN')).toBeInTheDocument();
    expect(screen.getByText('0.92')).toBeInTheDocument();
    expect(screen.getByText('Jun 15, 2026')).toBeInTheDocument();
  });

  it('should set score bar width from result score', () => {
    const result = mockSearchResponse.results[0];

    render(<ResultCard rank={1} result={result} isTopResult={false} />);

    const scoreFill = document.querySelector('.qv-score-fill') as HTMLElement;
    expect(scoreFill.style.width).toBe('92%');
  });

  it('should call onResultClick when card is activated', async () => {
    const result = mockSearchResponse.results[0];
    const onResultClick = vi.fn();

    render(
      <ResultCard rank={1} result={result} isTopResult={false} onClick={onResultClick} />,
    );

    screen.getByRole('button').click();

    expect(onResultClick).toHaveBeenCalledWith(result);
  });
});
