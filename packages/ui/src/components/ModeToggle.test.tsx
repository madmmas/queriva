import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MODE_TOGGLE_GROUP_LABEL, RAG_MODE_LABEL, SEARCH_MODE_LABEL } from '../constants/ui';
import { ModeToggle } from './ModeToggle';

afterEach(() => {
  cleanup();
});

describe('ModeToggle', () => {
  it('should change mode to rag when rag segment is clicked', async () => {
    const user = userEvent.setup();
    const onModeChange = vi.fn();

    render(<ModeToggle mode="search" onModeChange={onModeChange} />);

    const group = screen.getByRole('radiogroup', { name: MODE_TOGGLE_GROUP_LABEL });
    await user.click(within(group).getByRole('radio', { name: RAG_MODE_LABEL }));

    expect(onModeChange).toHaveBeenCalledWith('rag');
  });

  it('should mark search segment as checked when mode is search', () => {
    render(<ModeToggle mode="search" onModeChange={vi.fn()} />);

    const group = screen.getByRole('radiogroup', { name: MODE_TOGGLE_GROUP_LABEL });
    expect(within(group).getByRole('radio', { name: SEARCH_MODE_LABEL })).toHaveAttribute(
      'aria-checked',
      'true',
    );
  });
});
