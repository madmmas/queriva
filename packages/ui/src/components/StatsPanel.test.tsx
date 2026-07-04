import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { STAT_BEST_SCORE_LABEL, STAT_RESULTS_LABEL } from '../constants/ui';
import { StatsPanel } from './StatsPanel';

describe('StatsPanel', () => {
  it('should render metric cards from search stats', () => {
    render(
      <StatsPanel
        stats={{
          hitCount: 4,
          bestScore: 0.92,
          languageCount: 2,
          totalTimeMs: 1908,
        }}
      />,
    );

    expect(screen.getByText(STAT_RESULTS_LABEL)).toBeInTheDocument();
    expect(screen.getByText(STAT_BEST_SCORE_LABEL)).toBeInTheDocument();
    expect(screen.getByText('0.92')).toBeInTheDocument();
    expect(screen.getByText('1.9')).toBeInTheDocument();
  });
});
