import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import { afterEach, describe, expect, it } from 'vitest';
import {
  APP_NAME,
  RAG_MODE_LABEL,
  SEARCH_INPUT_LABEL,
  THEME_TOGGLE_LABEL,
} from './constants/ui';
import App from './App';

expect.extend(toHaveNoViolations);

afterEach(() => {
  cleanup();
  document.documentElement.removeAttribute('data-theme');
  localStorage.clear();
});

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
});
