import { cleanup, render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { afterEach, describe, expect, it } from 'vitest';
import App from './App';

expect.extend(toHaveNoViolations);

afterEach(() => {
  cleanup();
});

describe('App', () => {
  it('should render Queriva heading when app mounts', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: /Queriva/i })).toBeInTheDocument();
  });

  it('should have no accessibility violations when app mounts', async () => {
    const { container } = render(<App />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });
});
