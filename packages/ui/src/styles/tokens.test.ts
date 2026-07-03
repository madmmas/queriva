import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

describe('design tokens', () => {
  it('should define brand css variables matching mock ui colours', () => {
    const tokensPath = resolve(import.meta.dirname, '../styles/tokens.css');
    const tokensCss = readFileSync(tokensPath, 'utf8');

    expect(tokensCss).toContain('--qv-navy: #0d1b2a');
    expect(tokensCss).toContain('--qv-teal: #00c9b8');
    expect(tokensCss).toContain('--qv-amber: #f6ad55');
    expect(tokensCss).toContain("[data-theme='dark']");
    expect(tokensCss).toContain("[data-theme='light']");
  });
});
