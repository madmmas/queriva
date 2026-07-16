import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { SEARCH_INPUT_LABEL } from './constants/ui';
import { demoSearchResponse } from './fixtures/demoSearchResponse';
import { server } from './setupTests';
import SearchWidget from './SearchWidget';
import type { SearchResult } from './types/api';

afterEach(() => {
  cleanup();
});

describe('SearchWidget', () => {
  it('should render with only apiUrl and collection required props', () => {
    render(<SearchWidget apiUrl="" collection="news_radar" />);

    expect(screen.getByTestId('search-widget')).toBeInTheDocument();
    expect(screen.getByLabelText(SEARCH_INPUT_LABEL)).toBeInTheDocument();
  });

  it('should apply custom placeholder text', () => {
    render(
      <SearchWidget
        apiUrl=""
        collection="news_radar"
        placeholder="Search floods…"
      />,
    );

    expect(screen.getByLabelText(SEARCH_INPUT_LABEL)).toHaveAttribute(
      'placeholder',
      'Search floods…',
    );
  });

  it('should apply theme as data-theme on the widget root', () => {
    const { rerender } = render(
      <SearchWidget apiUrl="" collection="news_radar" theme="dark" />,
    );

    expect(screen.getByTestId('search-widget')).toHaveAttribute('data-theme', 'dark');

    rerender(<SearchWidget apiUrl="" collection="news_radar" theme="light" />);
    expect(screen.getByTestId('search-widget')).toHaveAttribute('data-theme', 'light');
  });

  it('should start in RAG mode when defaultMode is rag', () => {
    render(<SearchWidget apiUrl="" collection="news_radar" defaultMode="rag" />);

    expect(screen.getByRole('radio', { name: /RAG/i })).toHaveClass('on');
  });

  it('should pre-select language filter from filters prop', () => {
    render(
      <SearchWidget
        apiUrl=""
        collection="news_radar"
        filters={{ language: 'bn' }}
      />,
    );

    expect(screen.getByRole('button', { name: /Bangla/i })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('should call onResultClick with the clicked SearchResult', async () => {
    const user = userEvent.setup();
    const onResultClick = vi.fn();
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

    server.use(
      http.post('/api/search', () => HttpResponse.json(demoSearchResponse)),
    );

    render(
      <SearchWidget
        apiUrl=""
        collection="news_radar"
        onResultClick={onResultClick}
      />,
    );

    await user.type(screen.getByLabelText(SEARCH_INPUT_LABEL), 'floods in Dhaka{Enter}');

    await waitFor(() => {
      expect(screen.getByText(demoSearchResponse.results[0].title)).toBeInTheDocument();
    });

    await user.click(screen.getByText(demoSearchResponse.results[0].title));

    expect(onResultClick).toHaveBeenCalledTimes(1);
    const clicked = onResultClick.mock.calls[0][0] as SearchResult;
    expect(clicked.id).toBe(demoSearchResponse.results[0].id);
    expect(clicked.title).toBe(demoSearchResponse.results[0].title);
    expect(openSpy).not.toHaveBeenCalled();

    openSpy.mockRestore();
  });

  it('should open result URL when onResultClick is omitted', async () => {
    const user = userEvent.setup();
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

    server.use(
      http.post('/api/search', () => HttpResponse.json(demoSearchResponse)),
    );

    render(<SearchWidget apiUrl="" collection="news_radar" />);

    await user.type(screen.getByLabelText(SEARCH_INPUT_LABEL), 'floods in Dhaka{Enter}');

    await waitFor(() => {
      expect(screen.getByText(demoSearchResponse.results[0].title)).toBeInTheDocument();
    });

    await user.click(screen.getByText(demoSearchResponse.results[0].title));

    expect(openSpy).toHaveBeenCalledWith(
      demoSearchResponse.results[0].url,
      '_blank',
      'noopener,noreferrer',
    );

    openSpy.mockRestore();
  });
});
