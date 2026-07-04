import { http, HttpResponse } from 'msw';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import { afterEach, describe, expect, it } from 'vitest';
import { server } from './setupTests';
import {
  AI_SUMMARY_LABEL,
  APP_NAME,
  ERROR_BANNER_PREFIX,
  RAG_MODE_LABEL,
  SEARCH_INPUT_LABEL,
  SEARCH_SKELETON_LABEL,
  THEME_TOGGLE_LABEL,
} from './constants/ui';
import { demoSearchResponse } from './fixtures/demoSearchResponse';
import App from './App';

expect.extend(toHaveNoViolations);

const DHAKA_FLOODS_QUERY = 'floods in Dhaka last week';

afterEach(() => {
  cleanup();
  document.documentElement.removeAttribute('data-theme');
  localStorage.clear();
});

async function submitSearch(user: ReturnType<typeof userEvent.setup>, query: string) {
  const input = screen.getByLabelText(SEARCH_INPUT_LABEL);
  await user.clear(input);
  await user.type(input, query);
  await user.keyboard('{Enter}');
}

describe('App', () => {
  it('should render search zone with brand bar when app mounts', async () => {
    render(<App />);

    expect(screen.getByText(APP_NAME)).toBeInTheDocument();
    expect(screen.getByRole('banner')).toBeInTheDocument();
    expect(screen.getByLabelText(SEARCH_INPUT_LABEL)).toBeInTheDocument();

    await screen.findByText('qdrant');
  });

  it('should toggle data-theme when theme button is clicked', async () => {
    const user = userEvent.setup();
    render(<App />);

    document.documentElement.setAttribute('data-theme', 'light');
    await user.click(screen.getByRole('button', { name: THEME_TOGGLE_LABEL }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('should switch to rag mode when rag toggle is clicked', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('radio', { name: RAG_MODE_LABEL }));

    expect(screen.getByRole('radio', { name: RAG_MODE_LABEL })).toHaveAttribute(
      'aria-checked',
      'true',
    );
  });

  it('should have no accessibility violations when app mounts', async () => {
    const { container } = render(<App />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should show ranked results after search flow in search mode', async () => {
    const user = userEvent.setup();
    render(<App />);

    await submitSearch(user, DHAKA_FLOODS_QUERY);

    await waitFor(() =>
      expect(screen.getByText(demoSearchResponse.results[0].title)).toBeInTheDocument(),
    );
  });

  it('should show ai summary after rag search flow', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('radio', { name: RAG_MODE_LABEL }));
    await submitSearch(user, DHAKA_FLOODS_QUERY);

    await waitFor(() => expect(screen.getByText(AI_SUMMARY_LABEL)).toBeInTheDocument());
    expect(screen.getByText(/Three flood events hit Dhaka/)).toBeInTheDocument();
  });

  it('should show skeleton while search request is pending', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('/api/search', async () => {
        await new Promise(() => undefined);
        return HttpResponse.json(demoSearchResponse);
      }),
    );

    render(<App />);
    await submitSearch(user, DHAKA_FLOODS_QUERY);

    expect(await screen.findByLabelText(SEARCH_SKELETON_LABEL)).toBeInTheDocument();
  });

  it('should show error banner when search returns 500', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('/api/search', () =>
        HttpResponse.json({ error: 'Internal error' }, { status: 500 }),
      ),
    );

    render(<App />);
    await submitSearch(user, DHAKA_FLOODS_QUERY);

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(`${ERROR_BANNER_PREFIX} Internal error`),
    );
  });
});
