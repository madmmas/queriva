import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import App from './App';
import {
  FILTER_STRIP_LABEL,
  RESULTS_SECTION_LABEL,
  SEARCH_INPUT_LABEL,
  STAT_RESULTS_LABEL,
} from './constants/ui';
import SearchWidget from './SearchWidget';

afterEach(() => {
  cleanup();
  document.documentElement.removeAttribute('data-theme');
  localStorage.clear();
});

/**
 * Verifies App and SearchWidget share the same search UI surface (issue #26).
 */
describe('MFE shared search surface', () => {
  it('should render shared search controls in both App and SearchWidget', () => {
    const { unmount } = render(<App />);

    expect(screen.getByLabelText(SEARCH_INPUT_LABEL)).toBeInTheDocument();
    expect(screen.getByRole('group', { name: FILTER_STRIP_LABEL })).toBeInTheDocument();
    expect(screen.getByText(RESULTS_SECTION_LABEL)).toBeInTheDocument();
    expect(screen.getByText(STAT_RESULTS_LABEL)).toBeInTheDocument();

    unmount();

    render(<SearchWidget apiUrl="" collection="news_radar" />);

    expect(screen.getByLabelText(SEARCH_INPUT_LABEL)).toBeInTheDocument();
    expect(screen.getByRole('group', { name: FILTER_STRIP_LABEL })).toBeInTheDocument();
    expect(screen.getByText(RESULTS_SECTION_LABEL)).toBeInTheDocument();
    expect(screen.getByText(STAT_RESULTS_LABEL)).toBeInTheDocument();
  });
});
