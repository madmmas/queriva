import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderSummaryWithReferences } from './summaryContent';

describe('renderSummaryWithReferences', () => {
  it('should render inline reference badges for bracketed numbers', () => {
    render(<p>{renderSummaryWithReferences('Event noted. [1] More detail. [2]')}</p>);

    expect(screen.getByText('1')).toHaveClass('qv-ref');
    expect(screen.getByText('2')).toHaveClass('qv-ref');
    expect(screen.getByText(/Event noted\./)).toBeInTheDocument();
  });
});
