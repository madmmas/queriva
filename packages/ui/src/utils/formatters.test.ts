import { describe, expect, it } from 'vitest';
import { formatScore, scoreBarWidthPercent } from './formatters';

describe('formatters', () => {
  it('should format score bar width as score times one hundred percent', () => {
    expect(scoreBarWidthPercent(0.92)).toBe('92%');
    expect(scoreBarWidthPercent(0.877)).toBe('87.7%');
  });

  it('should format scores with two decimal places', () => {
    expect(formatScore(0.92)).toBe('0.92');
  });
});
