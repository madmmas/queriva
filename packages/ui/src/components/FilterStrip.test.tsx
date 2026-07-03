import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { LANGUAGE_FILTER_BANGLA } from '../constants/ui';
import { FILTER_LANGUAGE_BANGLA } from '../constants/filters';
import { EMPTY_ACTIVE_FILTERS } from '../utils/filterState';
import { FilterStrip } from './FilterStrip';

describe('FilterStrip', () => {
  it('should emit filter change when language chip is clicked', async () => {
    const user = userEvent.setup();
    const onFiltersChange = vi.fn();

    render(
      <FilterStrip
        collection="news_radar"
        filters={EMPTY_ACTIVE_FILTERS}
        resultCount={0}
        onFiltersChange={onFiltersChange}
      />,
    );

    await user.click(screen.getByRole('button', { name: LANGUAGE_FILTER_BANGLA }));

    expect(onFiltersChange).toHaveBeenCalledWith({
      ...EMPTY_ACTIVE_FILTERS,
      language: FILTER_LANGUAGE_BANGLA,
    });
  });
});
