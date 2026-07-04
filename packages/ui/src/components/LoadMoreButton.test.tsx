import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { LOAD_MORE_LABEL } from '../constants/ui';
import { LoadMoreButton } from './LoadMoreButton';

describe('LoadMoreButton', () => {
  it('should render nothing when remaining count is zero', () => {
    const { container } = render(<LoadMoreButton remainingCount={0} onLoadMore={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('should call onLoadMore when clicked', async () => {
    const user = userEvent.setup();
    const onLoadMore = vi.fn();

    render(<LoadMoreButton remainingCount={4} onLoadMore={onLoadMore} />);

    await user.click(screen.getByRole('button', { name: LOAD_MORE_LABEL }));

    expect(onLoadMore).toHaveBeenCalledTimes(1);
  });
});
