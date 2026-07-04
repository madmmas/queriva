import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MODE_TOGGLE_GROUP_LABEL, RAG_MODE_LABEL, SEARCH_INPUT_LABEL, SEARCH_MODE_LABEL } from '../constants/ui';
import { SearchBar } from './SearchBar';

afterEach(() => {
  cleanup();
});

describe('SearchBar', () => {
  it('should submit query on enter key', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(
      <SearchBar
        query="floods in Dhaka"
        mode="search"
        onQueryChange={vi.fn()}
        onModeChange={vi.fn()}
        onSubmit={onSubmit}
      />,
    );

    await user.click(screen.getByLabelText(SEARCH_INPUT_LABEL));
    await user.keyboard('{Enter}');

    expect(onSubmit).toHaveBeenCalledWith('floods in Dhaka');
  });

  it('should call onModeChange when rag segment is selected', async () => {
    const user = userEvent.setup();
    const onModeChange = vi.fn();

    render(
      <SearchBar
        query=""
        mode="search"
        onQueryChange={vi.fn()}
        onModeChange={onModeChange}
        onSubmit={vi.fn()}
      />,
    );

    await user.click(
      within(screen.getByRole('radiogroup', { name: MODE_TOGGLE_GROUP_LABEL })).getByRole(
        'radio',
        { name: RAG_MODE_LABEL },
      ),
    );

    expect(onModeChange).toHaveBeenCalledWith('rag');
  });

  it('should mark search segment as checked in search mode', () => {
    render(
      <SearchBar
        query=""
        mode="search"
        onQueryChange={vi.fn()}
        onModeChange={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    const group = screen.getByRole('radiogroup', { name: MODE_TOGGLE_GROUP_LABEL });
    expect(within(group).getByRole('radio', { name: SEARCH_MODE_LABEL })).toHaveAttribute(
      'aria-checked',
      'true',
    );
  });
});
