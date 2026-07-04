import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DEFAULT_SUGGESTIONS } from '../constants/suggestions';
import { SUGGESTIONS_LABEL } from '../constants/ui';
import { SuggestionsPanel } from './SuggestionsPanel';

afterEach(() => {
  cleanup();
});

describe('SuggestionsPanel', () => {
  it('should render follow-up suggestion buttons', () => {
    render(<SuggestionsPanel onSuggestionClick={vi.fn()} />);

    expect(screen.getByText(SUGGESTIONS_LABEL)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: DEFAULT_SUGGESTIONS[0].label })).toBeInTheDocument();
  });

  it('should call onSuggestionClick with query when button is clicked', async () => {
    const user = userEvent.setup();
    const onSuggestionClick = vi.fn();

    render(<SuggestionsPanel onSuggestionClick={onSuggestionClick} />);

    const suggestionButtons = screen.getAllByRole('button');
    await user.click(suggestionButtons[1]);

    expect(onSuggestionClick).toHaveBeenCalledWith(DEFAULT_SUGGESTIONS[1].query);
  });
});
