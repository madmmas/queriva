import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ERROR_BANNER_PREFIX } from '../constants/ui';
import { ErrorBanner } from './ErrorBanner';

describe('ErrorBanner', () => {
  it('should render alert with prefixed error message', () => {
    render(<ErrorBanner message="Internal error" />);

    expect(screen.getByRole('alert')).toHaveTextContent(`${ERROR_BANNER_PREFIX} Internal error`);
  });
});
