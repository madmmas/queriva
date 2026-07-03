import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import { afterEach, describe, expect, it } from 'vitest';
import { APP_NAME, THEME_TOGGLE_LABEL } from './constants/ui';
import App from './App';

expect.extend(toHaveNoViolations);

afterEach(() => {
  cleanup();
  document.documentElement.removeAttribute('data-theme');
  localStorage.clear();
});

describe('App', () => {
  it('should render brand bar with Queriva name when app mounts', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: APP_NAME })).toBeInTheDocument();
    expect(screen.getByRole('banner')).toBeInTheDocument();
  });

  it('should toggle data-theme when theme button is clicked', async () => {
    const user = userEvent.setup();
    render(<App />);

    document.documentElement.setAttribute('data-theme', 'light');
    await user.click(screen.getByRole('button', { name: THEME_TOGGLE_LABEL }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('should render brand colour swatches for design token verification', () => {
    render(<App />);

    expect(document.querySelector('.qv-swatch-navy')).toBeInTheDocument();
    expect(document.querySelector('.qv-swatch-teal')).toBeInTheDocument();
    expect(document.querySelector('.qv-swatch-amber')).toBeInTheDocument();
  });

  it('should have no accessibility violations when app mounts', async () => {
    const { container } = render(<App />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });
});
