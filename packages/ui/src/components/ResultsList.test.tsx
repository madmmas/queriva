import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { NO_RESULTS_MESSAGE, SEARCH_SKELETON_LABEL } from '../constants/ui';
import { ResultsList } from './ResultsList';

describe('ResultsList', () => {
  it('should show empty state when results are empty', () => {
    render(
      <ResultsList
        query="floods in Dhaka"
        results={[]}
        loading={false}
        remainingCount={0}
        onLoadMore={() => undefined}
      />,
    );

    expect(screen.getByText(NO_RESULTS_MESSAGE)).toBeInTheDocument();
  });

  it('should show skeleton while loading', () => {
    render(
      <ResultsList
        query=""
        results={[]}
        loading
        remainingCount={0}
        onLoadMore={() => undefined}
      />,
    );

    expect(screen.getByLabelText(SEARCH_SKELETON_LABEL)).toBeInTheDocument();
  });
});
