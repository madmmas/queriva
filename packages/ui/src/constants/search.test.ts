import { describe, expect, it } from 'vitest';
import { DEFAULT_MIN_SCORE, resolveMinScoreFromEnv } from './search';

describe('resolveMinScoreFromEnv', () => {
  it('should return DEFAULT_MIN_SCORE when env value is missing', () => {
    expect(resolveMinScoreFromEnv(undefined)).toBe(DEFAULT_MIN_SCORE);
    expect(resolveMinScoreFromEnv('')).toBe(DEFAULT_MIN_SCORE);
    expect(resolveMinScoreFromEnv('   ')).toBe(DEFAULT_MIN_SCORE);
  });

  it('should parse a valid VITE_SEARCH_MIN_SCORE value', () => {
    expect(resolveMinScoreFromEnv('0.40')).toBe(0.4);
    expect(resolveMinScoreFromEnv('0.30')).toBe(0.3);
  });

  it('should fall back when the value is non-numeric or out of range', () => {
    expect(resolveMinScoreFromEnv('abc')).toBe(DEFAULT_MIN_SCORE);
    expect(resolveMinScoreFromEnv('-0.1')).toBe(DEFAULT_MIN_SCORE);
    expect(resolveMinScoreFromEnv('1.5')).toBe(DEFAULT_MIN_SCORE);
  });
});
